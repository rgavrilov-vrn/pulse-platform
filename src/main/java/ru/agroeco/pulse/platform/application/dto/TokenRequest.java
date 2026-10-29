package ru.agroeco.pulse.platform.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "TokenRequest", description = "Учётные данные пользователя")
public record TokenRequest(

        @NotBlank
        @Schema(
                description = "Логин (CITEXT, регистронезависимый)",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String username,

        @NotBlank
        @Schema(
                description = "Пароль (plaintext, проверяется bcrypt на сервере)",
                example = "admin",
                requiredMode = Schema.RequiredMode.REQUIRED,
                format = "password"
        )
        String password
) {}