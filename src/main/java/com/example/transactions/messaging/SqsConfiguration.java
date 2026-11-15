package com.example.transactions.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Profile("aws")
public class SqsConfiguration {
    @Bean(destroyMethod = "close")
    SqsClient sqsClient() {
        // Region and credentials come from the Lambda environment.
        return SqsClient.builder().httpClient(UrlConnectionHttpClient.create()).build();
    }
}
