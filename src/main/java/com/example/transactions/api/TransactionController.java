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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import org.springframework.http.ResponseEntity;
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

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String REPLAYED = "Idempotent-Replayed";

    @PostMapping
    @Operation(operationId = "createTransaction", summary = "Create a transaction",
            description = "Stores the transaction as PENDING and queues it for asynchronous processing. "
                    + "Poll GET /transactions/{id} until the status is SUCCESS or FAILED. "
                    + "Send an Idempotency-Key to make retries safe: repeating a request with the same key and body "
                    + "returns the original transaction instead of creating a new one.")
    @ApiResponse(responseCode = "202", description = "Accepted; processing is asynchronous. A repeated Idempotency-Key "
            + "returns the existing transaction in its current state",
            headers = @Header(name = REPLAYED, description = "\"true\" when the response is a replay for a repeated "
                    + "Idempotency-Key", schema = @Schema(type = "string", allowableValues = "true")))
    @ApiResponse(responseCode = "400", description = "Validation failed, malformed JSON or malformed Idempotency-Key; "
            + "`errors` maps field or header to message",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    @ApiResponse(responseCode = "409", description = "Invoice already exists, or the Idempotency-Key was already used "
            + "with a different request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiProblem.class)))
    public ResponseEntity<TransactionResponse> create(
            @Parameter(in = ParameterIn.HEADER, name = IDEMPOTENCY_KEY, description = "Optional client-generated key, "
                    + "1-255 visible ASCII characters (a UUID per logical request is recommended)",
                    example = "6f1c2b9e-3d4a-4c3e-9a51-0d2f7b8e4c11")
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false)
            @Size(min = 1, max = 255) @Pattern(regexp = "[\\x21-\\x7E]+", message = "must be visible ASCII characters")
            String idempotencyKey,
            @Valid @RequestBody CreateRequest request) {
        var result = service.create(request.invoice(), request.amount(), request.phoneNumber(), idempotencyKey);
        var response = ResponseEntity.status(HttpStatus.ACCEPTED);
        if (result.replayed()) response.header(REPLAYED, "true");
        return response.body(TransactionResponse.from(result.transaction()));
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
