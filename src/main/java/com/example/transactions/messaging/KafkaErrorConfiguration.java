package com.example.transactions.messaging;

import com.example.transactions.domain.TransactionNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorConfiguration {
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        // Failed records go to <topic>.DLT on the same partition, so one bad message never blocks a partition.
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        // Transient failures (DB down, etc.) back off 1s -> 30s for up to ~5 minutes before dead-lettering.
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(30_000L);
        backOff.setMaxElapsedTime(300_000L);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Malformed or unknown IDs will never succeed: dead-letter immediately.
        handler.addNotRetryableExceptions(IllegalArgumentException.class, TransactionNotFoundException.class);
        return handler;
    }
}
