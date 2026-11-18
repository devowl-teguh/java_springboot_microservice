package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.transactions.domain.RequestFingerprint;
import com.example.transactions.domain.Transaction;
import com.example.transactions.domain.TransactionStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Design 0001 state machine: PENDING -> SUCCESS | FAILED, both terminal. */
class TransactionStateTest {
    private static Transaction pending() { return new Transaction("INV-1", new BigDecimal("1.00"), "0812"); }

    @Test void pendingCanSucceedOnce() {
        var tx = pending();
        assertThat(tx.complete()).isTrue();
        assertThat(tx.complete()).isFalse();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(tx.getCompletedAt()).isNotNull();
        assertThat(tx.getFailureReason()).isNull();
    }

    @Test void pendingCanFailWithReason() {
        var tx = pending();
        assertThat(tx.fail("Card declined")).isTrue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(tx.getFailureReason()).isEqualTo("Card declined");
        assertThat(tx.getCompletedAt()).isNotNull();
    }

    @Test void terminalStatesAreFinal() {
        var failed = pending();
        failed.fail("first");
        var firstCompletedAt = failed.getCompletedAt();
        assertThat(failed.complete()).isFalse();
        assertThat(failed.fail("second")).isFalse();
        assertThat(failed.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("first");
        assertThat(failed.getCompletedAt()).isEqualTo(firstCompletedAt);

        var succeeded = pending();
        succeeded.complete();
        assertThat(succeeded.fail("late failure")).isFalse();
        assertThat(succeeded.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test void failureReasonIsTruncatedToTheColumnSize() {
        var tx = pending();
        tx.fail("x".repeat(500));
        assertThat(tx.getFailureReason()).hasSize(200);
    }

    @Test void keyAndHashMustBeSetTogether() {
        assertThatThrownBy(() -> new Transaction("INV-1", BigDecimal.ONE, "0812", "key", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void fingerprintIgnoresWhitespaceAndAmountScale() {
        String a = RequestFingerprint.of("INV-1", new BigDecimal("12500"), "0812");
        assertThat(RequestFingerprint.of("  INV-1 ", new BigDecimal("12500.00"), " 0812")).isEqualTo(a);
        assertThat(RequestFingerprint.of("INV-1", new BigDecimal("12500.01"), "0812")).isNotEqualTo(a);
        assertThat(RequestFingerprint.of("INV-2", new BigDecimal("12500"), "0812")).isNotEqualTo(a);
        assertThat(a).hasSize(64).matches("[0-9a-f]+");
    }
}
