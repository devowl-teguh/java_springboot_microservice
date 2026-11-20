package com.example.transactions.domain;

import java.util.Optional;
import java.util.regex.Pattern;

/** Normalizes Indonesian phone numbers to the stored form 62XXXXXXXXX (Design 0002). */
public final class PhoneNumbers {
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-.()]");
    private static final Pattern NORMALIZED = Pattern.compile("62[1-9][0-9]{6,11}");
    public static final String FORMAT_MESSAGE = "must be an Indonesian phone number, e.g. 628123456789";

    private PhoneNumbers() {}

    /** "0812-3456-789", "+62 812 3456 789" and "628123456789" all become "628123456789"; anything else is empty. */
    public static Optional<String> normalize(String input) {
        if (input == null) return Optional.empty();
        String s = SEPARATORS.matcher(input.trim()).replaceAll("");
        if (s.startsWith("+")) s = s.substring(1);
        if (s.startsWith("0")) s = "62" + s.substring(1);
        return NORMALIZED.matcher(s).matches() ? Optional.of(s) : Optional.empty();
    }

    /** For callers that already validated the input (e.g. through {@code @IndonesianPhone}). */
    public static String normalizeOrThrow(String input) {
        return normalize(input).orElseThrow(() -> new IllegalArgumentException("phoneNumber " + FORMAT_MESSAGE));
    }
}
