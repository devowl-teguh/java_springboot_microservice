package com.example.transactions.messaging;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!aws")
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    public KafkaTransactionEventPublisher(KafkaTemplate<String, String> kafka, @Value("${app.kafka.topic}") String topic) { this.kafka = kafka; this.topic = topic; }
    @Override
    public void publish(UUID transactionId) throws Exception {
        String id = transactionId.toString();
        kafka.send(topic, id, id).get(10, TimeUnit.SECONDS);
    }
}
