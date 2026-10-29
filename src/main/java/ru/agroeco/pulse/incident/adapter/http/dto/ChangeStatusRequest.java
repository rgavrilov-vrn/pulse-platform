package ru.agroeco.pulse.incident.adapter.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentStatus;

@Schema(name = "ChangeStatusRequest")
public record ChangeStatusRequest(

        @NotNull
        @Schema(description = "Целевой статус", example = "CONFIRMED", requiredMode = Schema.RequiredMode.REQUIRED)
        IncidentStatus toStatus,

        @Size(max = 10_000)
        @Schema(description = "Комментарий к смене статуса", nullable = true)
        String comment
) {}