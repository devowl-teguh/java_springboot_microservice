package com.example.transactions.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import java.util.Map;

/**
 * Documentation-only schema for error responses. Spring's {@code ProblemDetail} serializes its extra properties
 * (such as {@code errors}) at the top level, which springdoc cannot infer from the class itself.
 */
@Schema(name = "ProblemDetail", description = "RFC 7807 problem details")
public record ApiProblem(
        @Schema(example = "about:blank") URI type,
        @Schema(example = "Bad Request") String title,
        @Schema(example = "400") int status,
        @Schema(example = "Request validation failed") String detail,
        @Schema(example = "/transactions") URI instance,
        @Schema(description = "Field name to validation message; present only on validation errors",
                example = "{\"amount\": \"must be greater than or equal to 0.01\"}") Map<String, String> errors) {}
