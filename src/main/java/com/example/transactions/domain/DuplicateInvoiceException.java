package com.example.transactions.domain;

public class DuplicateInvoiceException extends RuntimeException {
    public DuplicateInvoiceException(String invoice, Throwable cause) { super("Invoice already exists: " + invoice, cause); }
}
