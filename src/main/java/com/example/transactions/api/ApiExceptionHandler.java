package com.example.transactions.api;

import com.example.transactions.domain.DuplicateInvoiceException;
import com.example.transactions.domain.IdempotencyKeyMismatchException;
import com.example.transactions.domain.TransactionNotFoundException;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Extending ResponseEntityExceptionHandler renders Spring MVC's built-in errors (malformed JSON, bad path UUID,
// unsupported media type, ...) as RFC 7807 ProblemDetail too, so every error response has the same shape.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    ProblemDetail notFound(TransactionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Transaction not found");
    }

    @ExceptionHandler(DuplicateInvoiceException.class)
    ProblemDetail duplicate(DuplicateInvoiceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Invoice already exists");
    }

    @ExceptionHandler(IdempotencyKeyMismatchException.class)
    ProblemDetail idempotencyMismatch(IdempotencyKeyMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Constraint violations on headers and query parameters (e.g. Idempotency-Key, limit) get the same errors map. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> errors = new TreeMap<>();
        e.getParameterValidationResults().forEach(result -> {
            // A @Valid request body is validated here too once the method has other constraints: report its fields.
            if (result instanceof ParameterErrors body) {
                body.getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
                return;
            }
            var param = result.getMethodParameter();
            RequestHeader header = param.getParameterAnnotation(RequestHeader.class);
            RequestParam query = param.getParameterAnnotation(RequestParam.class);
            String name = header != null && !header.value().isEmpty() ? header.value()
                    : query != null && !query.value().isEmpty() ? query.value() : param.getParameterName();
            result.getResolvableErrors().stream().findFirst()
                    .ifPresent(error -> errors.putIfAbsent(name, error.getDefaultMessage()));
        });
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        // Field -> message; rejected values are deliberately omitted so request data is not echoed back.
        Map<String, String> errors = new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, HttpStatus.BAD_REQUEST, request);
    }
}
