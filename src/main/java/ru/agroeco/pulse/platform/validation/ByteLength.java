package ru.agroeco.pulse.platform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Проверяет длину значения в UTF-8 байтах.
 * Используется для BCrypt-паролей: BCrypt ограничен 72 байтами,
 * а @Size(max=…) считает char'ы — на кириллице/эмодзи падало в 500.
 */
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ByteLengthValidator.class)
public @interface ByteLength {
    int max();
    String message() default "must not exceed {max} bytes in UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}