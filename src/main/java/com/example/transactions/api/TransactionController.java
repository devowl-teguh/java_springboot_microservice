package com.example.transactions.api;

import com.example.transactions.api.dto.ApiProblem;
import com.example.transactions.api.dto.CreateRequest;
import com.example.transactions.api.dto.TransactionResponse;
import com.example.transactions.api.dto.TransactionSummary;
import com.example.transactions.domain.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Create transactions and track their asynchronous processing")
public class TransactionController {
    private static final String PROBLEM_JSON = "application/problem+json";

    private final TransactionService service;
    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "createTransaction", summary = "Create a transaction",
            description = "Stores the transaction as PENDING and queues it for asynchronous processing. "
                    + "Poll GET /transactions/{id} until the status becomes SUCCESS.")
    @ApiResponse(responseCode = "202", description = "Accepted; processing is asynchronous")
    @ApiResponse(responseCode = "400", description = "Validation failed or malformed JSON; `errors` maps field to message",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "409", description = "Invoice already exists",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public TransactionResponse create(@Valid @RequestBody CreateRequest request) {
        return TransactionResponse.from(service.create(request.invoice(), request.amount(), request.phoneNumber()));
    }

    @GetMapping
    @Operation(operationId = "listRecentTransactions", summary = "List recent transactions",
            description = "Newest first. Phone numbers are masked because this list is public.")
    @ApiResponse(responseCode = "200", description = "Recent transactions")
    @ApiResponse(responseCode = "400", description = "limit is outside 1..50",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public List<TransactionSummary> recent(@Parameter(description = "How many to return (1-50)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(TransactionService.MAX_RECENT) int limit) {
        return service.recent(limit).stream().map(TransactionSummary::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getTransaction", summary = "Get a transaction by ID")
    @ApiResponse(responseCode = "200", description = "Transaction found")
    @ApiResponse(responseCode = "400", description = "ID is not a valid UUID",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "404", description = "Transaction not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public TransactionResponse get(@Parameter(description = "Transaction ID returned on creation",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable UUID id) {
        return TransactionResponse.from(service.get(id));
    }
}
