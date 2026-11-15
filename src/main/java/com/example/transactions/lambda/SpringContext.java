package com.example.transactions.lambda;

import com.example.transactions.TransactionApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/** Starts one non-web Spring context per Lambda container (reused across invocations and captured by SnapStart). */
final class SpringContext {
    private static ConfigurableApplicationContext context;
    private SpringContext() {}
    static synchronized <T> T bean(Class<T> type) {
        if (context == null) {
            var app = new SpringApplication(TransactionApplication.class);
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setAdditionalProfiles("aws", "aws-worker");
            context = app.run();
        }
        return context.getBean(type);
    }
}
