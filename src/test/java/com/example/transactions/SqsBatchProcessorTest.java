package com.example.transactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.transactions.domain.TransactionNotFoundException;
import com.example.transactions.domain.TransactionService;
import com.example.transactions.messaging.SqsBatchProcessor;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SqsBatchProcessorTest {
    private static SQSEvent.SQSMessage message(String id, String body) {
        var m = new SQSEvent.SQSMessage(); m.setMessageId(id); m.setBody(body); return m;
    }

    @Test void reportsOnlyFailedMessages() {
        var service = mock(TransactionService.class);
        var ok = UUID.randomUUID();
        var missing = UUID.randomUUID();
        doThrow(new TransactionNotFoundException(missing)).when(service).process(missing);
        var event = new SQSEvent();
        event.setRecords(List.of(message("m1", ok.toString()), message("m2", "not-a-uuid"), message("m3", missing.toString())));

        var response = new SqsBatchProcessor(service).process(event);

        verify(service).process(ok);
        assertEquals(List.of("m2", "m3"), response.getBatchItemFailures().stream().map(f -> f.getItemIdentifier()).toList());
    }
}
