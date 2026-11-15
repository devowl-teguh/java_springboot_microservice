package com.example.transactions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.messaging.SqsBatchProcessor;

/** Triggered by SQS; marks each transaction SUCCESS and reports per-message failures. */
public class SqsConsumerLambdaHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {
    private static final SqsBatchProcessor processor = SpringContext.bean(SqsBatchProcessor.class);
    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) { return processor.process(event); }
}
