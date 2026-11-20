package com.example.transactions.api.validation;

import com.example.transactions.domain.PhoneNumbers;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valid when the value normalizes to an Indonesian number (see {@link PhoneNumbers}); null is left to @NotBlank. */
@Documented
@Constraint(validatedBy = IndonesianPhone.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndonesianPhone {
    String message() default PhoneNumbers.FORMAT_MESSAGE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<IndonesianPhone, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || value.isBlank() || PhoneNumbers.normalize(value).isPresent();
        }
    }
}
