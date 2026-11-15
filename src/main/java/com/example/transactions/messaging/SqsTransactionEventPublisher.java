package com.example.transactions.messaging;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@Profile("aws")
public class SqsTransactionEventPublisher implements TransactionEventPublisher {
    private final SqsClient sqs;
    private final String queueUrl;
    public SqsTransactionEventPublisher(SqsClient sqs, @Value("${app.sqs.queue-url}") String queueUrl) { this.sqs = sqs; this.queueUrl = queueUrl; }
    @Override
    public void publish(UUID transactionId) {
        // sendMessage is synchronous and throws if SQS does not accept the message.
        sqs.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(transactionId.toString()).build());
    }
}
