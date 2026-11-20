package com.example.transactions.api.dto;

import com.example.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "A transaction in the recent list; the phone number is masked because the list is public")
public record TransactionSummary(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "INV-001") String invoice,
        @Schema(example = "12500.00") BigDecimal amount,
        @Schema(example = "6281*****789") String phoneNumber,
        @Schema(allowableValues = {"PENDING", "SUCCESS", "FAILED"}, example = "SUCCESS") String status,
        @Schema(example = "2026-11-14T10:15:30Z") Instant createdAt) {
    public static TransactionSummary from(Transaction t) {
        return new TransactionSummary(t.getId(), t.getInvoice(), t.getAmount(), mask(t.getPhoneNumber()),
                t.getStatus().name(), t.getCreatedAt());
    }

    /** Keeps the first 4 and last 3 characters ("0812****789"); short values keep only the last 2. */
    public static String mask(String phone) {
        if (phone == null) return null;
        int n = phone.length();
        if (n <= 7) return "*".repeat(Math.max(0, n - 2)) + phone.substring(Math.max(0, n - 2));
        return phone.substring(0, 4) + "*".repeat(n - 7) + phone.substring(n - 3);
    }
}
