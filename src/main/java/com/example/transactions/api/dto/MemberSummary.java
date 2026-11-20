package com.example.transactions.api.dto;

import com.example.transactions.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "A member in the public list; the phone number is masked")
public record MemberSummary(
        @Schema(example = "b1c7e4a2-8f3d-4e5a-9c61-2d7f0a3b8e94") UUID id,
        @Schema(example = "6281*****789") String phoneNumber,
        @Schema(example = "2026-11-20T09:00:00Z") Instant createdAt) {
    public static MemberSummary from(Member m) {
        return new MemberSummary(m.getId(), TransactionSummary.mask(m.getPhoneNumber()), m.getCreatedAt());
    }
}
