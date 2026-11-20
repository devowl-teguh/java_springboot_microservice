package com.example.transactions.api.dto;

import com.example.transactions.domain.Member;
import com.example.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "A member with the full phone number and their most recent transactions (newest first, up to 50)")
public record MemberDetail(
        @Schema(example = "b1c7e4a2-8f3d-4e5a-9c61-2d7f0a3b8e94") UUID id,
        @Schema(description = "Normalized phone number", example = "628123456789") String phoneNumber,
        @Schema(example = "2026-11-20T09:00:00Z") Instant createdAt,
        List<TransactionSummary> transactions) {
    public static MemberDetail from(Member m, List<Transaction> history) {
        return new MemberDetail(m.getId(), m.getPhoneNumber(), m.getCreatedAt(),
                history.stream().map(TransactionSummary::from).toList());
    }
}
