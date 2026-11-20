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

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByOrderByCreatedAtDesc(Limit limit);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByMemberIdOrderByCreatedAtDesc(UUID memberId, Limit limit);

    /** Records the first hand-off to the broker; a re-published event keeps the original time. */
    @Modifying
    @Query("update Transaction t set t.publishedAt = :at where t.id = :id and t.publishedAt is null")
    int markPublished(@Param("id") UUID id, @Param("at") Instant at);
}
