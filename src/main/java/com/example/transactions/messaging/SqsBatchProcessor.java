package com.example.transactions.messaging;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.domain.TransactionService;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Processes an SQS batch and reports only the failed messages, so successful ones are not redelivered.
 * Failed messages return to the queue and move to the dead-letter queue after maxReceiveCount attempts.
 */
@Component
@Profile("aws")
public class SqsBatchProcessor {
    private static final Logger log = LoggerFactory.getLogger(SqsBatchProcessor.class);
    private final TransactionService service;
    public SqsBatchProcessor(TransactionService service) { this.service = service; }

    public SQSBatchResponse process(SQSEvent event) {
        var failures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
        for (var message : event.getRecords()) {
            try { service.process(UUID.fromString(message.getBody().trim())); }
            catch (Exception e) {
                log.warn("Failed to process message {}", message.getMessageId(), e);
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }
}
