package com.example.transactions.api.dto;

import com.example.transactions.api.validation.IndonesianPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Request to create a transaction")
public record CreateRequest(
        @Schema(description = "Unique invoice number; reusing one returns 409", example = "INV-001")
        @NotBlank @Size(max = 100) String invoice,
        @Schema(description = "Positive amount with at most 2 decimal places", example = "12500.00")
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Schema(description = "Indonesian phone number. Accepts 0812…, +62 812… or 62812… (spaces, dashes, dots and "
                + "parentheses are ignored); stored and returned as 62…, and used to find or create the member",
                example = "08123456789")
        @NotBlank @Size(max = 30) @IndonesianPhone String phoneNumber) {}
