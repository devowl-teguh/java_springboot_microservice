package com.example.transactions.domain;

/** PENDING until processed; SUCCESS and FAILED are terminal (Design 0001). */
public enum TransactionStatus {
    PENDING, SUCCESS, FAILED;

    public boolean isTerminal() { return this != PENDING; }
}
