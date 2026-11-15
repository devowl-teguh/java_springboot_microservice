package com.example.transactions.messaging;
import com.example.transactions.domain.TransactionService;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
@org.springframework.context.annotation.Profile("!aws")
public class TransactionConsumer {
    private final TransactionService service;
    public TransactionConsumer(TransactionService service) { this.service = service; }
    @KafkaListener(topics = "${app.kafka.topic}")
    public void consume(String transactionId) { service.process(UUID.fromString(transactionId)); }
}
