package com.example.transactions.messaging;

import java.util.UUID;

/** Port for sending "transaction created" events. Kafka locally, SQS on AWS Lambda. */
public interface TransactionEventPublisher {
    /** Blocks until the broker acknowledges the event; throws if it was not accepted. */
    void publish(UUID transactionId) throws Exception;
}
