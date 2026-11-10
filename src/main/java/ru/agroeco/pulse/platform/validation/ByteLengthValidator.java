package ru.agroeco.pulse.platform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class ByteLengthValidator implements ConstraintValidator<ByteLength, CharSequence> {

    private int max;

    @Override
    public void initialize(ByteLength annotation) {
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
    }
}