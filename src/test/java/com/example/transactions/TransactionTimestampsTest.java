package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.transactions.api.dto.TransactionSummary;
import com.example.transactions.domain.Transaction;
import com.example.transactions.domain.TransactionStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionTimestampsTest {
    @Test void completeIsIdempotentAndKeepsFirstCompletionTime() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("1.00"), "0812");
        assertThat(tx.getCompletedAt()).isNull();
        tx.complete();
        var first = tx.getCompletedAt();
        Thread.sleep(5);
        tx.complete();
        assertThat(tx.getCompletedAt()).isEqualTo(first);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test void phoneMasking() {
        assertThat(TransactionSummary.mask("08123456789")).isEqualTo("0812****789");
        assertThat(TransactionSummary.mask("+6281234567890")).isEqualTo("+628*******890");
        assertThat(TransactionSummary.mask("12345678")).isEqualTo("1234*678");
        assertThat(TransactionSummary.mask("0812")).isEqualTo("**12");
        assertThat(TransactionSummary.mask("1")).isEqualTo("1");
        assertThat(TransactionSummary.mask(null)).isNull();
    }
}
