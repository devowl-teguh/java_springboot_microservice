package com.example.transactions.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class Outbox {
    @Id private UUID id;
    @Column(name = "transaction_id", nullable = false) private UUID transactionId;
    @Column(nullable = false) private boolean published;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Outbox() {}
    public Outbox(UUID transactionId) { this.id = UUID.randomUUID(); this.transactionId = transactionId; this.createdAt = Instant.now(); }
    public UUID getTransactionId() { return transactionId; }
    public void markPublished() { published = true; }
}
