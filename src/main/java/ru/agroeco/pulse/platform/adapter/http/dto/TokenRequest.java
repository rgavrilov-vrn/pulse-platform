package ru.agroeco.pulse.platform.adapter.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// Пример задается на уровне всего класса
@Schema(
        name = "TokenRequest",
        description = "Учётные данные пользователя",
        example = "{\"username\":\"admin\",\"password\":\"admin\"}"
)
public record TokenRequest(

        @NotBlank
        @Schema(description = "Логин (CITEXT, регистронезависимый)", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @NotBlank
        @Schema(description = "Пароль (plaintext, проверяется bcrypt на сервере)", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String password
) {}