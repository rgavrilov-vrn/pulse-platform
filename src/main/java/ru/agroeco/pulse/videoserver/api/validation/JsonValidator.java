package ru.agroeco.pulse.videoserver.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonValidator implements ConstraintValidator<ValidJson, String> {

    private final ObjectMapper mapper;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) {
            return true; // пусть @NotBlank ругается
        }
        try {
            mapper.readTree(value);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}