package com.example.transactions.messaging;

import com.example.transactions.domain.OutboxRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka, @Value("${app.kafka.topic}") String topic) { this.outbox = outbox; this.kafka = kafka; this.topic = topic; }
    @Scheduled(fixedDelayString = "${OUTBOX_DELAY_MS:1000}")
    @Transactional
    public void publish() {
        for (var event : outbox.lockPending()) {
            String id = event.getTransactionId().toString();
            try { kafka.send(topic, id, id).get(10, TimeUnit.SECONDS); event.markPublished(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Publishing interrupted", e); }
            catch (Exception e) { log.warn("Outbox publish failed; batch will retry", e); throw new IllegalStateException("Outbox publish failed", e); }
        }
    }
}
