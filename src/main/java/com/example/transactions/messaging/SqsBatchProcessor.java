package com.example.transactions.messaging;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.domain.TransactionService;
import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final int maxReceiveCount;

    /** maxReceiveCount must match the queue's redrive policy in template.yaml. */
    public SqsBatchProcessor(TransactionService service, @Value("${app.sqs.max-receive-count:3}") int maxReceiveCount) {
        this.service = service; this.maxReceiveCount = maxReceiveCount;
    }

    public SQSBatchResponse process(SQSEvent event) {
        var failures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
        for (var message : event.getRecords()) {
            try { service.process(UUID.fromString(message.getBody().trim())); }
            catch (Exception e) {
                log.warn("Failed to process message {}", message.getMessageId(), e);
                if (isLastAttempt(message)) markFailed(message);
                // Still reported as failed, so SQS moves it to the dead-letter queue for inspection.
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }

    private boolean isLastAttempt(SQSEvent.SQSMessage message) {
        var attributes = message.getAttributes();
        String count = attributes == null ? null : attributes.get("ApproximateReceiveCount");
        try { return count != null && Integer.parseInt(count) >= maxReceiveCount; }
        catch (NumberFormatException e) { return false; }
    }

    private void markFailed(SQSEvent.SQSMessage message) {
        try { service.markFailed(UUID.fromString(message.getBody().trim()), TransactionService.RETRIES_EXHAUSTED); }
        catch (Exception e) { log.warn("Could not mark message {} as FAILED", message.getMessageId(), e); }
    }
}
