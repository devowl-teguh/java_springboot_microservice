package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.example.transactions.lambda.ApiLambdaHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/** Feeds Function URL (payload v2) events through the real Lambda handler, backed by real Postgres. */
class ApiLambdaHandlerIT {
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static ApiLambdaHandler handler;

    @BeforeAll static void start() {
        postgres.start();
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("SQS_QUEUE_URL", "https://sqs.example/queue");
        System.setProperty("aws.region", "us-east-1");
        handler = new ApiLambdaHandler();
    }
    @AfterAll static void stop() { postgres.stop(); }

    private String call(AwsProxyRequestBuilder request) throws Exception {
        var out = new ByteArrayOutputStream();
        handler.handleRequest(new ByteArrayInputStream(request.toHttpApiV2RequestStream().readAllBytes()), out, new MockLambdaContext());
        return out.toString();
    }

    @Test void swaggerUiAssetsArriveIntact() throws Exception {
        String response = call(new AwsProxyRequestBuilder("/swagger-ui/swagger-ui-bundle.js", "GET"));
        String body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("body").asText();
        // Compare with the original file from the swagger-ui webjar: any re-encoding changes multi-byte characters.
        assertThat(body).isEqualTo(locateBundle());
    }

    private static String locateBundle() throws Exception {
        var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        var bundle = resolver.getResources("classpath*:META-INF/resources/webjars/swagger-ui/*/swagger-ui-bundle.js")[0];
        return new String(bundle.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test void createThenGetThroughLambda() throws Exception {
        String created = call(new AwsProxyRequestBuilder("/transactions", "POST")
                .header("Content-Type", "application/json")
                .body("{\"invoice\":\"INV-LAMBDA-1\",\"amount\":10.00,\"phoneNumber\":\"0812\"}"));
        assertThat(created).contains("\"statusCode\":202").contains("PENDING");

        String id = created.replaceAll("(?s).*\\\\\"id\\\\\":\\\\\"([0-9a-f-]{36}).*", "$1");
        assertThat(call(new AwsProxyRequestBuilder("/transactions/" + id, "GET"))).contains("\"statusCode\":200").contains("INV-LAMBDA-1");
        assertThat(call(new AwsProxyRequestBuilder("/transactions/00000000-0000-0000-0000-000000000000", "GET"))).contains("\"statusCode\":404");
    }
}
