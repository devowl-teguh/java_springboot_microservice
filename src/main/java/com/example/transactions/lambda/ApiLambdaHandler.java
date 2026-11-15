package com.example.transactions.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.example.transactions.TransactionApplication;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Runs the Spring MVC controllers behind a Lambda Function URL (payload format 2.0). */
public class ApiLambdaHandler implements RequestStreamHandler {
    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, ?> handler;
    static {
        // Responses without a charset (e.g. Swagger UI's JavaScript) are otherwise re-encoded as ISO-8859-1,
        // which corrupts multi-byte characters.
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset("UTF-8");
        try { handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(TransactionApplication.class, "aws"); }
        catch (ContainerInitializationException e) { throw new IllegalStateException("Could not start Spring Boot", e); }
    }
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        handler.proxyStream(input, output, context);
    }
}
