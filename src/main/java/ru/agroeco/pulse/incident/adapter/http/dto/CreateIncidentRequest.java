package ru.agroeco.pulse.incident.adapter.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentCreationMode;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;

import java.time.Instant;
import java.util.Map;

@Schema(name = "CreateIncidentRequest")
public record CreateIncidentRequest(

        @Schema(description = "Код источника (incident.sources.code). По умолчанию 'manual'.",
                example = "manual", nullable = true)
        String source,

        @NotNull
        @Schema(description = "Способ создания", example = "MANUAL", requiredMode = Schema.RequiredMode.REQUIRED)
        IncidentCreationMode creationMode,

        @NotNull
        @Schema(description = "Момент события (ISO-8601)", example = "2026-02-11T10:15:30Z",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Instant eventTime,

        @Schema(description = "Серьёзность (по умолчанию MEDIUM)", example = "HIGH", nullable = true)
        IncidentSeverity severity,

        @Size(max = 10_000)
        @Schema(description = "Свободное описание", nullable = true)
        String comment,

        @Schema(description = "Расширяемые атрибуты", nullable = true)
        Map<String, Object> metadata
) {}