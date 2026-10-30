package ru.agroeco.pulse.videoserver.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Запрос на создание видеосервера.
 * <p>Уникальность пары (source, externalId) гарантируется БД — при дубликате
 * вернётся 422 с {@code VIDEO_SERVER_ALREADY_EXISTS}.</p>
 */
public record CreateVideoServerRequest(
        UUID objectId,
        UUID zoneId,
        @NotBlank @Size(max = 64)  String source,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String host,
        @Size(max = 512) String baseUrl,
        String description,
        @Size(max = 32) String status,        // ONLINE / OFFLINE / UNKNOWN, default UNKNOWN
        Boolean isActive                      // default TRUE
) {
}