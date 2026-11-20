package com.example.transactions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.transactions.api.TransactionController;
import com.example.transactions.domain.CreateResult;
import com.example.transactions.domain.DuplicateInvoiceException;
import com.example.transactions.domain.IdempotencyKeyMismatchException;
import com.example.transactions.domain.Transaction;
import com.example.transactions.domain.TransactionNotFoundException;
import com.example.transactions.domain.TransactionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired MockMvc mvc;
    @MockitoBean TransactionService service;

    @Test void validRequestIsAccepted() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("12500.00"), "08123456789");
        when(service.create(any(), any(), any(), isNull())).thenReturn(new CreateResult(tx, false));
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":12500.00,\"phoneNumber\":\"08123456789\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(tx.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
    }

    private static final String BODY = "{\"invoice\":\"INV-1\",\"amount\":12500.00,\"phoneNumber\":\"08123456789\"}";

    @Test void repeatedIdempotencyKeyIsAReplay() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("12500.00"), "08123456789");
        when(service.create(any(), any(), any(), eq("key-1"))).thenReturn(new CreateResult(tx, true));
        mvc.perform(post("/transactions").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(tx.getId().toString()));
    }

    @Test void reusedKeyWithDifferentBodyReturns409() throws Exception {
        when(service.create(any(), any(), any(), eq("key-1"))).thenThrow(new IdempotencyKeyMismatchException());
        mvc.perform(post("/transactions").header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Idempotency-Key was already used with a different request"));
    }

    @Test void malformedIdempotencyKeyReturns400WithHeaderError() throws Exception {
        mvc.perform(post("/transactions").header("Idempotency-Key", "has space")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors['Idempotency-Key']").exists());
        mvc.perform(post("/transactions").header("Idempotency-Key", "k".repeat(256))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void failedTransactionIncludesReason() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("1.00"), "08123456789");
        tx.fail("Amount exceeds the processor limit of 100,000,000.00");
        when(service.get(tx.getId())).thenReturn(tx);
        mvc.perform(get("/transactions/" + tx.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Amount exceeds the processor limit of 100,000,000.00"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test void invalidFieldsReturnProblemDetailWithFieldErrors() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"\",\"amount\":0,\"phoneNumber\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.invoice").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists());
        verifyNoInteractions(service);
    }

    @Test void tooManyFractionDigitsIsRejected() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":1.001,\"phoneNumber\":\"08123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test void nonIndonesianPhoneIsRejected() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":1.00,\"phoneNumber\":\"+1 415 555 0100\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phoneNumber").value("must be an Indonesian phone number, e.g. 628123456789"));
        verifyNoInteractions(service);
    }

    @Test void transactionIncludesMemberId() throws Exception {
        var memberId = UUID.randomUUID();
        var tx = new Transaction("INV-1", new BigDecimal("1.00"), "628123456789", null, null, memberId);
        when(service.get(tx.getId())).thenReturn(tx);
        mvc.perform(get("/transactions/" + tx.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId.toString()))
                .andExpect(jsonPath("$.phoneNumber").value("628123456789"));
    }

    @Test void malformedJsonReturnsProblemDetail() throws Exception {
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test void invalidUuidReturnsProblemDetail() throws Exception {
        mvc.perform(get("/transactions/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("id")));
    }

    @Test void unknownIdReturns404ProblemDetail() throws Exception {
        var id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new TransactionNotFoundException(id));
        mvc.perform(get("/transactions/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Transaction not found"));
    }

    @Test void recentListMasksPhoneNumbers() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("1.00"), "08123456789");
        when(service.recent(10)).thenReturn(List.of(tx));
        mvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoice").value("INV-1"))
                .andExpect(jsonPath("$[0].phoneNumber").value("0812****789"));
    }

    @Test void recentLimitOutOfRangeReturnsProblemDetail() throws Exception {
        mvc.perform(get("/transactions").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.limit").exists());
        mvc.perform(get("/transactions").param("limit", "51"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void detailIncludesProcessingTimestamps() throws Exception {
        var tx = new Transaction("INV-1", new BigDecimal("1.00"), "08123456789");
        tx.complete();
        when(service.get(tx.getId())).thenReturn(tx);
        mvc.perform(get("/transactions/" + tx.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.phoneNumber").value("08123456789"));
    }

    @Test void duplicateInvoiceReturns409ProblemDetail() throws Exception {
        when(service.create(any(), any(), any(), any())).thenThrow(new DuplicateInvoiceException("INV-1", null));
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":1.00,\"phoneNumber\":\"08123456789\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invoice already exists"));
    }
}
