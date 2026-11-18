package com.example.transactions.domain;

/** Outcome of a create request: the transaction, and whether it was replayed for a repeated Idempotency-Key. */
public record CreateResult(Transaction transaction, boolean replayed) {}
