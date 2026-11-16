package com.example.transactions.api.dto;

import com.example.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "A transaction and its processing status")
public record TransactionResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "INV-001") String invoice,
        @Schema(example = "12500.00") BigDecimal amount,
        @Schema(example = "08123456789") String phoneNumber,
        @Schema(description = "PENDING until processed, then SUCCESS", allowableValues = {"PENDING", "SUCCESS"},
                example = "PENDING") String status,
        @Schema(example = "2026-11-14T10:15:30Z") Instant createdAt,
        @Schema(description = "When the outbox relay handed the event to the broker; null until then",
                example = "2026-11-14T10:16:00Z", nullable = true) Instant publishedAt,
        @Schema(description = "When processing finished; null while PENDING",
                example = "2026-11-14T10:16:02Z", nullable = true) Instant completedAt) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getInvoice(), transaction.getAmount(),
                transaction.getPhoneNumber(), transaction.getStatus(), transaction.getCreatedAt(),
                transaction.getPublishedAt(), transaction.getCompletedAt());
    }
}
