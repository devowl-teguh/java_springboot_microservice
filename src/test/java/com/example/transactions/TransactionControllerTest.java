package com.example.transactions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.transactions.api.TransactionController;
import com.example.transactions.domain.DuplicateInvoiceException;
import com.example.transactions.domain.Transaction;
import com.example.transactions.domain.TransactionNotFoundException;
import com.example.transactions.domain.TransactionService;
import java.math.BigDecimal;
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
        when(service.create(any(), any(), any())).thenReturn(tx);
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":12500.00,\"phoneNumber\":\"08123456789\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(tx.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
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
                        .content("{\"invoice\":\"INV-1\",\"amount\":1.001,\"phoneNumber\":\"0812\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
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

    @Test void duplicateInvoiceReturns409ProblemDetail() throws Exception {
        when(service.create(any(), any(), any())).thenThrow(new DuplicateInvoiceException("INV-1", null));
        mvc.perform(post("/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":\"INV-1\",\"amount\":1.00,\"phoneNumber\":\"0812\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invoice already exists"));
    }
}
