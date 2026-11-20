package com.example.transactions.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    /**
     * Atomic find-or-create, step 1: concurrent first transactions for the same phone do not raise a unique violation
     * (which would abort the surrounding transaction); the losers simply insert nothing.
     */
    @Modifying
    @Query(value = "INSERT INTO transaction_service.members (id, phone_number, created_at) VALUES (:id, :phone, :at) "
            + "ON CONFLICT (phone_number) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("phone") String phoneNumber, @Param("at") Instant createdAt);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    List<Member> findAllByOrderByCreatedAtDesc(Limit limit);
}
