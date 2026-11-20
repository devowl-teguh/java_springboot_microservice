package com.example.transactions.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    public static final int MAX_LIST = 50;
    public static final int MAX_HISTORY = 50;

    private final MemberRepository members;
    private final TransactionRepository transactions;

    public MemberService(MemberRepository members, TransactionRepository transactions) {
        this.members = members; this.transactions = transactions;
    }

    /**
     * Returns the member for a normalized phone number, creating it if needed. Must run inside the caller's transaction,
     * so the member commits (or rolls back) together with the transaction that needed it.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID findOrCreate(String normalizedPhoneNumber) {
        members.insertIfAbsent(UUID.randomUUID(), normalizedPhoneNumber, Instant.now());
        return members.findByPhoneNumber(normalizedPhoneNumber)
                .orElseThrow(() -> new IllegalStateException("member must exist after insertIfAbsent"))
                .getId();
    }

    /** Newest members first; the limit is clamped to 1..MAX_LIST. */
    @Transactional(readOnly = true)
    public List<Member> recent(int limit) {
        return members.findAllByOrderByCreatedAtDesc(Limit.of(Math.max(1, Math.min(limit, MAX_LIST))));
    }

    @Transactional(readOnly = true)
    public Member get(UUID id) { return members.findById(id).orElseThrow(() -> new MemberNotFoundException(id)); }

    /** The member's most recent transactions, newest first. */
    @Transactional(readOnly = true)
    public List<Transaction> history(UUID memberId) {
        return transactions.findByMemberIdOrderByCreatedAtDesc(memberId, Limit.of(MAX_HISTORY));
    }
}
