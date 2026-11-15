package com.example.transactions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.example.transactions.messaging.OutboxPublisher;
import java.util.Map;

/** Invoked by an EventBridge schedule: relays pending outbox rows to SQS. */
public class OutboxLambdaHandler implements RequestHandler<Map<String, Object>, String> {
    private static final OutboxPublisher publisher = SpringContext.bean(OutboxPublisher.class);
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        int total = 0, sent;
        // Drain several batches per run so a backlog clears faster than one batch per minute.
        do { sent = publisher.publish(); total += sent; } while (sent > 0 && total < 1000 && context.getRemainingTimeInMillis() > 10_000);
        return "published " + total;
    }
}
