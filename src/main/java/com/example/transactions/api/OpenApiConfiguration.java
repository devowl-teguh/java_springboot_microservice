package com.example.transactions.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI transactionOpenApi(@Value("${app.openapi.server-url}") String serverUrl) {
        // An explicit server keeps the generated spec stable (springdoc otherwise derives it from each request),
        // and gives API clients like Postman a ready-to-use base URL after import.
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction API")
                        .version("v1")
                        .description("Records transactions and processes them asynchronously via a transactional "
                                + "outbox and Kafka. Creation returns 202 with status PENDING; poll GET until SUCCESS. "
                                + "Errors are RFC 7807 ProblemDetail (application/problem+json)."))
                .servers(List.of(new Server().url(serverUrl).description("Default server")));
    }
}
