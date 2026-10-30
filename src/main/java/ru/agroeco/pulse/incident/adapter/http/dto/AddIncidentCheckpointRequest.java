package ru.agroeco.pulse.incident.adapter.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AddIncidentCheckpointRequest")
public record AddIncidentCheckpointRequest(

        @NotBlank @Size(max = 100)
        @Schema(description = "Код пункта чек-листа (incident.checklist_items.code)",
                example = "TRANSPORT_OPERATIONS", requiredMode = Schema.RequiredMode.REQUIRED)
        String checkpoint,

        @Size(max = 10_000)
        @Schema(description = "Комментарий к привязке", nullable = true)
        String comment
) {}