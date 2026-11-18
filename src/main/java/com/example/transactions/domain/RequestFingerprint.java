package com.example.transactions.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 of the canonical create request, stored next to the Idempotency-Key (Design 0001). */
public final class RequestFingerprint {
    private RequestFingerprint() {}

    /** Text is trimmed and the amount compared at scale 2, so "12500" and "12500.00" are the same request. */
    public static String of(String invoice, BigDecimal amount, String phoneNumber) {
        String canonical = invoice.trim() + "\n" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + "\n" + phoneNumber.trim();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
