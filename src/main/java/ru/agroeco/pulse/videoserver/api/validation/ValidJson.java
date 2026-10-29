package ru.agroeco.pulse.videoserver.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = JsonValidator.class)
public @interface ValidJson {
    String message() default "must be valid JSON";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}