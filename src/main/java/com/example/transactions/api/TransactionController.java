package com.example.transactions.api;

import com.example.transactions.api.dto.CreateRequest;
import com.example.transactions.api.dto.TransactionResponse;
import com.example.transactions.domain.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionResponse create(@Valid @RequestBody CreateRequest request) {
        return TransactionResponse.from(service.create(request.invoice(), request.amount(), request.phoneNumber()));
    }
    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) { return TransactionResponse.from(service.get(id)); }
}
