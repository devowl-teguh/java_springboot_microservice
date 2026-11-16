package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.domain.TransactionService;
import com.example.transactions.messaging.OutboxPublisher;
import com.example.transactions.messaging.SqsBatchProcessor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/** Boots the AWS profile (no Kafka) against real Postgres, with SQS mocked. */
@Testcontainers
@ActiveProfiles({"aws", "aws-worker"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "SQS_QUEUE_URL=https://sqs.example/queue")
class AwsModeIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean SqsClient sqs;
    @Autowired TransactionService service;
    @Autowired OutboxPublisher outbox;
    @Autowired SqsBatchProcessor processor;

    @Test
    void outboxGoesToSqsAndConsumerCompletesTransaction() {
        var tx = service.create("INV-AWS-1", new BigDecimal("9.99"), "0812");

        assertThat(outbox.publish()).isEqualTo(1);
        var sent = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqs).sendMessage(sent.capture());
        assertThat(sent.getValue().queueUrl()).isEqualTo("https://sqs.example/queue");
        assertThat(sent.getValue().messageBody()).isEqualTo(tx.getId().toString());
        assertThat(outbox.publish()).isZero();

        var message = new SQSEvent.SQSMessage();
        message.setMessageId("m1");
        message.setBody(sent.getValue().messageBody());
        var event = new SQSEvent();
        event.setRecords(List.of(message));
        assertThat(processor.process(event).getBatchItemFailures()).isEmpty();
        var done = service.get(tx.getId());
        assertThat(done.getStatus()).isEqualTo("SUCCESS");
        assertThat(done.getPublishedAt()).isNotNull();
        assertThat(done.getCompletedAt()).isAfterOrEqualTo(done.getPublishedAt());
        assertThat(service.recent(5)).extracting(t -> t.getId()).contains(tx.getId());
    }
}
