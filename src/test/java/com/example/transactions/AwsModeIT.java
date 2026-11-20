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
import com.example.transactions.domain.DuplicateInvoiceException;
import com.example.transactions.domain.MemberService;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        var tx = service.create("INV-AWS-1", new BigDecimal("9.99"), "08123456789");

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
                return service.create("INV-RACE-" + key, new BigDecimal("5.00"), "08123456789", key);
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
        var tx = service.create("INV-AWS-REJECT", new BigDecimal("150000000.00"), "08123456789");
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

    @Autowired MemberService members;

    private int memberCount(String phone) {
        return jdbc.queryForObject("select count(*) from transaction_service.members where phone_number = ?", Integer.class, phone);
    }

    @Test
    void firstTransactionCreatesMemberAndLaterFormatsReuseIt() {
        String suffix = String.valueOf(System.nanoTime() % 100_000_000);
        String phone = "62812" + String.format("%08d", Long.parseLong(suffix)).substring(0, 7);
        var first = service.create("INV-M1-" + suffix, new BigDecimal("1.00"), "0" + phone.substring(2));
        var second = service.create("INV-M2-" + suffix, new BigDecimal("2.00"), "+62 " + phone.substring(2, 5) + "-" + phone.substring(5));

        assertThat(first.getPhoneNumber()).isEqualTo(phone);
        assertThat(second.getMemberId()).isEqualTo(first.getMemberId()).isNotNull();
        assertThat(memberCount(phone)).isEqualTo(1);
        assertThat(members.history(first.getMemberId())).extracting(t -> t.getInvoice())
                .containsExactly("INV-M2-" + suffix, "INV-M1-" + suffix);
    }

    @Test
    void concurrentFirstTransactionsForOnePhoneCreateOneMember() throws Exception {
        String phone = "62813" + String.format("%07d", System.nanoTime() % 10_000_000);
        int threads = 8;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(threads);
        var futures = new ArrayList<Future<com.example.transactions.domain.Transaction>>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            futures.add(pool.submit(() -> {
                start.await();
                return service.create("INV-MRACE-" + phone + "-" + n, new BigDecimal("1.00"), phone);
            }));
        }
        start.countDown();
        var memberIds = new java.util.HashSet<java.util.UUID>();
        for (var f : futures) memberIds.add(f.get().getMemberId());
        pool.shutdown();

        assertThat(memberIds).hasSize(1);
        assertThat(memberCount(phone)).isEqualTo(1);
    }

    @Test
    void failedCreateLeavesNoOrphanMember() {
        String phone = "62814" + String.format("%07d", System.nanoTime() % 10_000_000);
        service.create("INV-ORPHAN-" + phone, new BigDecimal("1.00"), "08123456789");
        assertThatThrownBy(() -> service.create("INV-ORPHAN-" + phone, new BigDecimal("1.00"), phone))
                .isInstanceOf(DuplicateInvoiceException.class);
        assertThat(memberCount(phone)).isZero();
    }

    @Test
    void idempotentReplayTreatsLocalAndCountryCodeFormsAsTheSameBody() {
        String key = "fmt-" + java.util.UUID.randomUUID();
        var first = service.create("INV-FMT-" + key, new BigDecimal("3.00"), "08123456789", key);
        var again = service.create("INV-FMT-" + key, new BigDecimal("3.00"), "628123456789", key);
        assertThat(again.replayed()).isTrue();
        assertThat(again.transaction().getId()).isEqualTo(first.transaction().getId());
    }
}
