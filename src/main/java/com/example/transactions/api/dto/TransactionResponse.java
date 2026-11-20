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
        @Schema(description = "Normalized phone number", example = "628123456789") String phoneNumber,
        @Schema(description = "PENDING until processed, then SUCCESS or FAILED (both terminal)",
                allowableValues = {"PENDING", "SUCCESS", "FAILED"}, example = "PENDING") String status,
        @Schema(example = "2026-11-14T10:15:30Z") Instant createdAt,
        @Schema(description = "When the outbox relay handed the event to the broker; null until then",
                example = "2026-11-14T10:16:00Z", nullable = true) Instant publishedAt,
        @Schema(description = "When the transaction reached SUCCESS or FAILED; null while PENDING",
                example = "2026-11-14T10:16:02Z", nullable = true) Instant completedAt,
        @Schema(description = "Why the transaction failed; present only when FAILED",
                example = "Amount exceeds the processor limit of 100,000,000.00", nullable = true) String failureReason,
        @Schema(description = "Member who made the transaction; null for transactions created before members existed",
                example = "b1c7e4a2-8f3d-4e5a-9c61-2d7f0a3b8e94", nullable = true) UUID memberId) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getInvoice(), transaction.getAmount(),
                transaction.getPhoneNumber(), transaction.getStatus().name(), transaction.getCreatedAt(),
                transaction.getPublishedAt(), transaction.getCompletedAt(), transaction.getFailureReason(),
                transaction.getMemberId());
    }
}
