package com.example.transactions.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    public static final int MAX_RECENT = 50;
    public static final String RETRIES_EXHAUSTED = "Processing failed after retries";

    private final TransactionRepository transactions;
    private final OutboxRepository outbox;
    private final MemberService members;
    private final TransactionOperations tx;
    private final BigDecimal processorLimit;

    public TransactionService(TransactionRepository transactions, OutboxRepository outbox, MemberService members,
                              TransactionOperations tx,
                              @Value("${app.processor-limit:100000000.00}") BigDecimal processorLimit) {
        this.transactions = transactions; this.outbox = outbox; this.members = members; this.tx = tx;
        this.processorLimit = processorLimit;
    }

    public Transaction create(String invoice, BigDecimal amount, String phoneNumber) {
        return create(invoice, amount, phoneNumber, null).transaction();
    }

    /**
     * Creates a transaction and its outbox row in one database transaction. With an Idempotency-Key, a repeated request
     * returns the existing transaction (replayed) and a different request with the same key is rejected.
     * Not @Transactional on purpose: a unique violation aborts its transaction, so the follow-up lookup needs a new one.
     */
    public CreateResult create(String invoice, BigDecimal amount, String rawPhoneNumber, String idempotencyKey) {
        // Design 0002: store, return and fingerprint the normalized number, so 0812… and 62812… are the same person.
        String phoneNumber = PhoneNumbers.normalizeOrThrow(rawPhoneNumber);
        String hash = idempotencyKey == null ? null : RequestFingerprint.of(invoice, amount, phoneNumber);
        if (idempotencyKey != null) {
            Optional<Transaction> existing = findByKey(idempotencyKey);
            if (existing.isPresent()) return replay(existing.get(), hash);
        }
        try {
            Transaction created = tx.execute(status -> {
                // Flush now so a duplicate invoice or key fails here, not at commit time.
                // Member, transaction and outbox row commit together: a failed insert leaves no orphan member.
                UUID memberId = members.findOrCreate(phoneNumber);
                Transaction t = transactions.saveAndFlush(
                        new Transaction(invoice, amount, phoneNumber, idempotencyKey, hash, memberId));
                outbox.save(new Outbox(t.getId()));
                return t;
            });
            return new CreateResult(created, false);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent request using the same key, or the invoice is taken.
            if (idempotencyKey != null) {
                Optional<Transaction> existing = findByKey(idempotencyKey);
                if (existing.isPresent()) return replay(existing.get(), hash);
            }
            throw new DuplicateInvoiceException(invoice, e);
        }
    }

    private Optional<Transaction> findByKey(String key) {
        return tx.execute(status -> transactions.findByIdempotencyKey(key));
    }

    private static CreateResult replay(Transaction existing, String hash) {
        if (!existing.getRequestHash().equals(hash)) throw new IdempotencyKeyMismatchException();
        return new CreateResult(existing, true);
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID id) { return transactions.findById(id).orElseThrow(() -> new TransactionNotFoundException(id)); }

    /** Newest transactions first; the limit is clamped to 1..MAX_RECENT. */
    @Transactional(readOnly = true)
    public List<Transaction> recent(int limit) {
        return transactions.findAllByOrderByCreatedAtDesc(Limit.of(Math.max(1, Math.min(limit, MAX_RECENT))));
    }

    /**
     * Processes the event for a transaction. Terminal transactions are left alone, so redelivery is safe. Amounts above
     * the (simulated) processor limit are a business rejection: FAILED at once, never retried.
     */
    @Transactional
    public void process(UUID id) {
        Transaction t = get(id);
        if (t.getStatus().isTerminal()) {
            log.info("Ignoring event for transaction {}: already {}", id, t.getStatus());
            return;
        }
        if (t.getAmount().compareTo(processorLimit) > 0) {
            t.fail("Amount exceeds the processor limit of " + String.format("%,.2f", processorLimit));
            log.info("Transaction {} rejected: amount above processor limit", id);
        } else {
            t.complete();
        }
    }

    /** Called on the last delivery attempt after technical failures. Unknown or terminal transactions are ignored. */
    @Transactional
    public void markFailed(UUID id, String reason) {
        transactions.findById(id).ifPresent(t -> {
            if (t.fail(reason)) log.warn("Transaction {} marked FAILED: {}", id, reason);
        });
    }
}
