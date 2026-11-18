package com.example.transactions.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    static final int MAX_FAILURE_REASON = 200;

    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String invoice;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "phone_number", nullable = false, length = 30) private String phoneNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TransactionStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    /** When the transaction reached a terminal state (SUCCESS or FAILED). */
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "idempotency_key", unique = true, length = 255) private String idempotencyKey;
    @Column(name = "request_hash", length = 64) private String requestHash;
    @Column(name = "failure_reason", length = MAX_FAILURE_REASON) private String failureReason;

    protected Transaction() {}

    public Transaction(String invoice, BigDecimal amount, String phoneNumber) {
        this(invoice, amount, phoneNumber, null, null);
    }

    public Transaction(String invoice, BigDecimal amount, String phoneNumber, String idempotencyKey, String requestHash) {
        if ((idempotencyKey == null) != (requestHash == null)) {
            throw new IllegalArgumentException("idempotencyKey and requestHash must be set together");
        }
        this.id = UUID.randomUUID(); this.invoice = invoice; this.amount = amount; this.phoneNumber = phoneNumber;
        this.status = TransactionStatus.PENDING; this.createdAt = Instant.now();
        this.idempotencyKey = idempotencyKey; this.requestHash = requestHash;
    }

    /** PENDING -> SUCCESS. Idempotent; ignored once the transaction is terminal (a redelivered event changes nothing). */
    public boolean complete() {
        if (status.isTerminal()) return false;
        status = TransactionStatus.SUCCESS;
        completedAt = Instant.now();
        return true;
    }

    /** PENDING -> FAILED with a user-facing reason. Ignored once terminal. */
    public boolean fail(String reason) {
        if (status.isTerminal()) return false;
        status = TransactionStatus.FAILED;
        failureReason = reason.length() > MAX_FAILURE_REASON ? reason.substring(0, MAX_FAILURE_REASON) : reason;
        completedAt = Instant.now();
        return true;
    }

    public UUID getId() { return id; }
    public String getInvoice() { return invoice; }
    public BigDecimal getAmount() { return amount; }
    public String getPhoneNumber() { return phoneNumber; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getFailureReason() { return failureReason; }
}
