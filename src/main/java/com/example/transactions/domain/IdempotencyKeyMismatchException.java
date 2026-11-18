package com.example.transactions.domain;

/** The Idempotency-Key was already used for a request with a different body. */
public class IdempotencyKeyMismatchException extends RuntimeException {
    public IdempotencyKeyMismatchException() { super("Idempotency-Key was already used with a different request"); }
}
