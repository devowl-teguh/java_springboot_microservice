package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.transactions.domain.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;

/** Service-level rules from Design 0001; concurrency against real Postgres is covered in AwsModeIT. */
class IdempotentCreateTest {
    private static final BigDecimal AMOUNT = new BigDecimal("12500.00");
    /** Stored (normalized) form; requests below use the local form 0812…, which must count as the same request. */
    private static final String PHONE = "628123456789";
    private static final String LOCAL = "08123456789";
    private TransactionRepository repo;
    private OutboxRepository outbox;
    private MemberService members;
    private TransactionService service;

    @BeforeEach void setUp() {
        repo = mock(TransactionRepository.class);
        outbox = mock(OutboxRepository.class);
        members = mock(MemberService.class);
        when(repo.saveAndFlush(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        service = new TransactionService(repo, outbox, members, TransactionOperations.withoutTransaction(), new BigDecimal("100000000.00"));
    }

    private static Transaction stored(String key, String invoice, BigDecimal amount) {
        return new Transaction(invoice, amount, PHONE, key, RequestFingerprint.of(invoice, amount, PHONE));
    }

    @Test void newKeyCreatesTransactionAndOutboxEvent() {
        var memberId = java.util.UUID.randomUUID();
        when(members.findOrCreate(PHONE)).thenReturn(memberId);
        var result = service.create("INV-1", AMOUNT, LOCAL, "key-1");
        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getPhoneNumber()).isEqualTo(PHONE);
        assertThat(result.transaction().getMemberId()).isEqualTo(memberId);
        assertThat(result.transaction().getIdempotencyKey()).isEqualTo("key-1");
        verify(outbox).save(any(Outbox.class));
    }

    @Test void repeatedKeyWithSameBodyReplaysWithoutWriting() {
        var existing = stored("key-1", "INV-1", AMOUNT);
        when(repo.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        var result = service.create(" INV-1 ", new BigDecimal("12500"), "+62 812-3456-789", "key-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.transaction()).isSameAs(existing);
        verify(repo, never()).saveAndFlush(any());
        verifyNoInteractions(outbox);
    }

    @Test void repeatedKeyWithDifferentBodyIsRejected() {
        when(repo.findByIdempotencyKey("key-1")).thenReturn(Optional.of(stored("key-1", "INV-1", AMOUNT)));
        assertThatThrownBy(() -> service.create("INV-1", new BigDecimal("1.00"), LOCAL, "key-1"))
                .isInstanceOf(IdempotencyKeyMismatchException.class);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test void losingAConcurrentRaceReplaysTheWinner() {
        var winner = stored("key-1", "INV-1", AMOUNT);
        when(repo.findByIdempotencyKey("key-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique idempotency_key"));

        var result = service.create("INV-1", AMOUNT, LOCAL, "key-1");

        assertThat(result.replayed()).isTrue();
        assertThat(result.transaction()).isSameAs(winner);
    }

    @Test void takenInvoiceWithANewKeyIsStillAConflict() {
        when(repo.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(repo.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique invoice"));
        assertThatThrownBy(() -> service.create("INV-1", AMOUNT, LOCAL, "key-2"))
                .isInstanceOf(DuplicateInvoiceException.class);
    }

    @Test void withoutKeyNothingIsLookedUp() {
        service.create("INV-1", AMOUNT, LOCAL, null);
        verify(repo, never()).findByIdempotencyKey(any());
    }

    @Test void amountAboveProcessorLimitFailsWithoutRetry() {
        var tx = new Transaction("INV-1", new BigDecimal("100000000.01"), "0812");
        when(repo.findById(tx.getId())).thenReturn(Optional.of(tx));

        service.process(tx.getId());

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getFailureReason()).isEqualTo("Amount exceeds the processor limit of 100,000,000.00");
    }

    @Test void amountAtTheLimitSucceeds() {
        var tx = new Transaction("INV-1", new BigDecimal("100000000.00"), "0812");
        when(repo.findById(tx.getId())).thenReturn(Optional.of(tx));
        service.process(tx.getId());
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test void markFailedLeavesSuccessfulTransactionsAlone() {
        var tx = new Transaction("INV-1", AMOUNT, "0812");
        tx.complete();
        when(repo.findById(tx.getId())).thenReturn(Optional.of(tx));
        service.markFailed(tx.getId(), TransactionService.RETRIES_EXHAUSTED);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }
}
