package com.example.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.transactions.api.dto.TransactionSummary;
import com.example.transactions.domain.PhoneNumbers;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Design 0002 phone rules. */
class PhoneNumbersTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "08123456789        | 628123456789",
            "+62 812-3456-789   | 628123456789",
            "628123456789       | 628123456789",
            "6285544632         | 6285544632",
            "(0812) 3456.789    | 628123456789",
            "  0812 3456 789    | 628123456789",
            "+62812345678901    | 62812345678901",
            "6281234567       | 6281234567"})
    void normalizesAcceptedForms(String input, String expected) {
        assertThat(PhoneNumbers.normalize(input)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "+1 415 555 0100",      // another country
            "12345",                // too short, no country code
            "62 0812 3456 789",     // national part may not start with 0
            "62812345",             // 6 national digits: too short
            "6281234567890123456",  // 13+ national digits: too long
            "0812abc456",           // letters
            "++628123456789",
            "   "})
    void rejectsEverythingElse(String input) {
        assertThat(PhoneNumbers.normalize(input)).isEqualTo(Optional.empty());
    }

    @ParameterizedTest
    @CsvSource({"628123456789, 6281*****789", "6285544632, 6285***632"})
    void masksNormalizedNumbers(String phone, String masked) {
        assertThat(TransactionSummary.mask(phone)).isEqualTo(masked);
    }
}
