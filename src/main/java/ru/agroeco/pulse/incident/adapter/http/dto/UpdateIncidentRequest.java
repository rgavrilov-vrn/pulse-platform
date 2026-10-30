package ru.agroeco.pulse.incident.adapter.http.dto;

import jakarta.validation.constraints.Size;
import ru.agroeco.pulse.incident.domain.IncidentKind;

import java.time.Instant;

public record UpdateIncidentRequest(
        IncidentKind kind,
        Instant eventTime,
        @Size(max = 10_000) String comment,
        @Size(max = 100) String checkpoint   // код checklist_items.code — добавляет link, идемпотентно
) {}