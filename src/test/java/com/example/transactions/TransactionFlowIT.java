package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "OUTBOX_DELAY_MS=200")
class TransactionFlowIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @Autowired TestRestTemplate http;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;

    record Tx(UUID id, String invoice, String status) {}

    @Test
    void createdTransactionIsProcessedToSuccess() {
        var created = http.postForEntity("/transactions",
                Map.of("invoice", "INV-IT-1", "amount", "12500.00", "phoneNumber", "08123456789"), Tx.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(created.getBody().status()).isEqualTo("PENDING");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(http.getForObject("/transactions/" + created.getBody().id(), Tx.class).status())
                        .isEqualTo("SUCCESS"));
    }

    @Test
    void duplicateInvoiceReturns409() {
        var body = Map.of("invoice", "INV-IT-DUP", "amount", "1.00", "phoneNumber", "0812");
        assertThat(http.postForEntity("/transactions", body, String.class).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(http.postForEntity("/transactions", body, String.class).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void openApiSpecAndSwaggerUiAreServed() {
        var spec = http.getForEntity("/v3/api-docs", String.class);
        assertThat(spec.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(spec.getBody()).contains("\"createTransaction\"", "\"getTransaction\"");
        assertThat(http.getForEntity("/swagger-ui/index.html", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unknownIdReturns404() {
        assertThat(http.getForEntity("/transactions/" + UUID.randomUUID(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void poisonMessagesAreDeadLetteredAndDoNotBlockProcessing() throws Exception {
        // Same key -> same partition, so a blocked partition would stall the valid transaction behind it.
        kafkaTemplate.send("transactions-v1", "poison", "not-a-uuid").get();
        kafkaTemplate.send("transactions-v1", "poison", UUID.randomUUID().toString()).get();

        var created = http.postForEntity("/transactions",
                Map.of("invoice", "INV-IT-AFTER-POISON", "amount", "5.00", "phoneNumber", "0812"), Tx.class);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(http.getForObject("/transactions/" + created.getBody().id(), Tx.class).status())
                        .isEqualTo("SUCCESS"));

        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-check",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(java.util.List.of("transactions-v1.DLT"));
            var values = new java.util.ArrayList<String>();
            await().atMost(Duration.ofSeconds(30)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> values.add(r.value()));
                return values.contains("not-a-uuid") && values.size() >= 2;
            });
        }
    }
}
