package com.example.transactions.api;

import com.example.transactions.domain.DuplicateInvoiceException;
import com.example.transactions.domain.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    ProblemDetail notFound(TransactionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Transaction not found");
    }
    @ExceptionHandler(DuplicateInvoiceException.class)
    ProblemDetail duplicate(DuplicateInvoiceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Invoice already exists");
    }
}
