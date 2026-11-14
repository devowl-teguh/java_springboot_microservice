package com.example.transactions.domain;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
    @Query(value = "SELECT * FROM transaction_service.outbox WHERE published = false ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> lockPending();
}
