package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.domain.TransactionService;
import com.example.transactions.domain.TransactionStatus;
import com.example.transactions.messaging.OutboxPublisher;
import com.example.transactions.messaging.SqsBatchProcessor;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import com.example.transactions.domain.CreateResult;
import com.example.transactions.domain.OutboxRepository;
import com.example.transactions.domain.TransactionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
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
        assertThat(done.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(done.getPublishedAt()).isNotNull();
        assertThat(done.getCompletedAt()).isAfterOrEqualTo(done.getPublishedAt());
        assertThat(service.recent(5)).extracting(t -> t.getId()).contains(tx.getId());
    }

    @Autowired TransactionRepository transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void concurrentRequestsWithOneKeyCreateExactlyOneTransactionAndEvent() throws Exception {
        String key = "race-" + java.util.UUID.randomUUID();
        int threads = 8;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        var futures = new ArrayList<Future<CreateResult>>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return service.create("INV-RACE-" + key, new BigDecimal("5.00"), "0812", key);
            }));
        }
        start.countDown();
        var results = new ArrayList<CreateResult>();
        for (var f : futures) results.add(f.get());
        pool.shutdown();

        assertThat(results).extracting(r -> r.transaction().getId()).containsOnly(results.get(0).transaction().getId());
        assertThat(results).filteredOn(r -> !r.replayed()).hasSize(1);
        var id = results.get(0).transaction().getId();
        assertThat(jdbc.queryForObject("select count(*) from transaction_service.transactions where idempotency_key = ?",
                Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from transaction_service.outbox where transaction_id = ?",
                Integer.class, id)).isEqualTo(1);
    }

    @Test
    void rejectedAmountEndsFailedThroughSqsPath() {
        var tx = service.create("INV-AWS-REJECT", new BigDecimal("150000000.00"), "0812");
        outbox.publish();
        var message = new SQSEvent.SQSMessage();
        message.setMessageId("m-reject");
        message.setBody(tx.getId().toString());
        var event = new SQSEvent();
        event.setRecords(List.of(message));

        assertThat(processor.process(event).getBatchItemFailures()).isEmpty();
        var done = service.get(tx.getId());
        assertThat(done.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(done.getFailureReason()).startsWith("Amount exceeds the processor limit");
    }
}
