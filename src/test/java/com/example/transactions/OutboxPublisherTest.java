package com.example.transactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.transactions.domain.Outbox;
import com.example.transactions.domain.OutboxRepository;
import com.example.transactions.messaging.OutboxPublisher;
import com.example.transactions.messaging.TransactionEventPublisher;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxPublisherTest {
    @Test void publishesEveryPendingEventAndMarksIt() throws Exception {
        var repo = mock(OutboxRepository.class);
        var broker = mock(TransactionEventPublisher.class);
        var a = new Outbox(UUID.randomUUID());
        var b = new Outbox(UUID.randomUUID());
        when(repo.lockPending()).thenReturn(List.of(a, b));

        assertEquals(2, new OutboxPublisher(repo, broker).publish());
        verify(broker).publish(a.getTransactionId());
        verify(broker).publish(b.getTransactionId());
    }

    @Test void failureRollsBackTheBatch() throws Exception {
        var repo = mock(OutboxRepository.class);
        var broker = mock(TransactionEventPublisher.class);
        when(repo.lockPending()).thenReturn(List.of(new Outbox(UUID.randomUUID())));
        doThrow(new RuntimeException("broker down")).when(broker).publish(any());

        assertThrows(IllegalStateException.class, () -> new OutboxPublisher(repo, broker).publish());
    }
}
