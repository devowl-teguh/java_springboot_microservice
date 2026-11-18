package com.example.transactions.messaging;

import com.example.transactions.domain.TransactionNotFoundException;
import com.example.transactions.domain.TransactionService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@org.springframework.context.annotation.Profile("!aws")
public class KafkaErrorConfiguration {
    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfiguration.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template, TransactionService service) {
        // Failed records go to <topic>.DLT on the same partition, so one bad message never blocks a partition.
        var deadLetter = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        // Runs once retries are exhausted (or at once for non-retryable errors): record FAILED, then dead-letter.
        org.springframework.kafka.listener.ConsumerRecordRecoverer recoverer = (record, ex) -> {
            markFailed(service, String.valueOf(record.value()));
            deadLetter.accept(record, ex);
        };
        // Transient failures (DB down, etc.) back off 1s -> 30s for up to ~5 minutes before dead-lettering.
        var backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(30_000L);
        backOff.setMaxElapsedTime(300_000L);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        // Malformed or unknown IDs will never succeed: dead-letter immediately.
        handler.addNotRetryableExceptions(IllegalArgumentException.class, TransactionNotFoundException.class);
        return handler;
    }

    /** Best effort: a malformed ID or a database outage must not stop the record from reaching the DLT. */
    static void markFailed(TransactionService service, String value) {
        try {
            service.markFailed(UUID.fromString(value.trim()), TransactionService.RETRIES_EXHAUSTED);
        } catch (Exception e) {
            log.warn("Could not mark transaction {} as FAILED", value, e);
        }
    }
}
