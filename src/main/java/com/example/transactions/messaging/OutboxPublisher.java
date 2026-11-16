package com.example.transactions.messaging;

import com.example.transactions.domain.OutboxRepository;
import com.example.transactions.domain.TransactionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Relays pending outbox rows to the configured broker. Triggered by a scheduler (Kafka mode) or a Lambda (AWS mode). */
@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final TransactionRepository transactions;
    private final TransactionEventPublisher publisher;
    public OutboxPublisher(OutboxRepository outbox, TransactionRepository transactions, TransactionEventPublisher publisher) {
        this.outbox = outbox; this.transactions = transactions; this.publisher = publisher;
    }

    /** Publishes one batch and returns how many events were sent. */
    @Transactional
    public int publish() {
        var pending = outbox.lockPending();
        for (var event : pending) {
            try {
                publisher.publish(event.getTransactionId());
                event.markPublished();
                transactions.markPublished(event.getTransactionId(), Instant.now());
            }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Publishing interrupted", e); }
            catch (Exception e) { log.warn("Outbox publish failed; batch will retry", e); throw new IllegalStateException("Outbox publish failed", e); }
        }
        return pending.size();
    }
}
