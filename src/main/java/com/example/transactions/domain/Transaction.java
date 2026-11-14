package com.example.transactions.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String invoice;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "phone_number", nullable = false, length = 30) private String phoneNumber;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected Transaction() {}
    public Transaction(String invoice, BigDecimal amount, String phoneNumber) {
        this.id = UUID.randomUUID(); this.invoice = invoice; this.amount = amount;
        this.phoneNumber = phoneNumber; this.status = "PENDING"; this.createdAt = Instant.now();
    }
    public void complete() { this.status = "SUCCESS"; }
    public UUID getId() { return id; }
    public String getInvoice() { return invoice; }
    public BigDecimal getAmount() { return amount; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
