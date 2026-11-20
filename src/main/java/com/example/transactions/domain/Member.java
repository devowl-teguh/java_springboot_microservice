package com.example.transactions.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A person who has transacted, identified by a normalized phone number (Design 0002). */
@Entity
@Table(name = "members")
public class Member {
    @Id private UUID id;
    @Column(name = "phone_number", nullable = false, unique = true, length = 20) private String phoneNumber;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Member() {}

    public Member(String normalizedPhoneNumber) {
        this.id = UUID.randomUUID(); this.phoneNumber = normalizedPhoneNumber; this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public Instant getCreatedAt() { return createdAt; }
}
