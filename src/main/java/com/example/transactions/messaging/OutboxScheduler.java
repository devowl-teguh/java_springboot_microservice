package com.example.transactions.messaging;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Long-running mode: poll the outbox every second. On AWS an EventBridge schedule invokes OutboxLambdaHandler instead. */
@Component
@Profile("!aws")
public class OutboxScheduler {
    private final OutboxPublisher publisher;
    public OutboxScheduler(OutboxPublisher publisher) { this.publisher = publisher; }
    @Scheduled(fixedDelayString = "${OUTBOX_DELAY_MS:1000}")
    public void run() { publisher.publish(); }
}
