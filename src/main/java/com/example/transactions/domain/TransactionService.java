package com.example.transactions.domain;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Limit;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final OutboxRepository outbox;
    public TransactionService(TransactionRepository transactions, OutboxRepository outbox) { this.transactions = transactions; this.outbox = outbox; }
    @Transactional
    public Transaction create(String invoice, BigDecimal amount, String phoneNumber) {
        Transaction transaction;
        try {
            // Flush now so a duplicate invoice fails here, not at commit time.
            transaction = transactions.saveAndFlush(new Transaction(invoice, amount, phoneNumber));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateInvoiceException(invoice, e);
        }
        outbox.save(new Outbox(transaction.getId()));
        return transaction;
    }
    @Transactional(readOnly = true)
    public Transaction get(UUID id) { return transactions.findById(id).orElseThrow(() -> new TransactionNotFoundException(id)); }
    public static final int MAX_RECENT = 50;

    /** Newest transactions first; the limit is clamped to 1..MAX_RECENT. */
    @Transactional(readOnly = true)
    public List<Transaction> recent(int limit) {
        return transactions.findAllByOrderByCreatedAtDesc(Limit.of(Math.max(1, Math.min(limit, MAX_RECENT))));
    }

    @Transactional
    public void process(UUID id) { get(id).complete(); }
}
