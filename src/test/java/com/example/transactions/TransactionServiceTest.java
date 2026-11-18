package com.example.transactions;

import com.example.transactions.domain.*;
import java.math.BigDecimal;
import org.springframework.transaction.support.TransactionOperations;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    @Test void processingTwiceIsIdempotent() {
        var repository = mock(TransactionRepository.class);
        var outbox = mock(OutboxRepository.class);
        var transaction = new Transaction("INV-1", new BigDecimal("10.00"), "08123");
        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        var service = new TransactionService(repository, outbox, TransactionOperations.withoutTransaction(), new BigDecimal("100000000.00"));
        service.process(transaction.getId());
        service.process(transaction.getId());
        assertEquals(TransactionStatus.SUCCESS, transaction.getStatus());
    }
    @Test void creationQueuesAnOutboxRecord() {
        var repository = mock(TransactionRepository.class);
        var outbox = mock(OutboxRepository.class);
        when(repository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = new TransactionService(repository, outbox, TransactionOperations.withoutTransaction(), new BigDecimal("100000000.00")).create("INV-1", new BigDecimal("10.00"), "08123");
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        verify(outbox).save(any(Outbox.class));
    }
}
