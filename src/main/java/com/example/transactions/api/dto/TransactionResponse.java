package com.example.transactions.api.dto;

import com.example.transactions.domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(UUID id, String invoice, BigDecimal amount, String phoneNumber,
                                  String status, Instant createdAt) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getInvoice(), transaction.getAmount(),
                transaction.getPhoneNumber(), transaction.getStatus(), transaction.getCreatedAt());
    }
}
