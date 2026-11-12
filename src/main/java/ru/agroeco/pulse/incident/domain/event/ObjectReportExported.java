package ru.agroeco.pulse.incident.domain.event;

import ru.agroeco.pulse.incident.adapter.http.report.ObjectReportController;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** File generated on the server; does not assert delivery to the client. */
public record ObjectReportExported(
        UUID actorId, String format, ObjectReportController.Filter filters,
        int rowCount, long sizeBytes, Instant occurredAt
) implements DomainEvent {
    @Override public DomainEventType eventType() { return DomainEventType.OBJECT_REPORT_EXPORTED; }
    @Override public UUID entityId() { return null; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public boolean publishToOutbox() { return false; }
    @Override public Object newDataSnapshot() {
        return Map.of("reportType", "OBJECT_INCIDENTS", "format", format,
                "filters", filters, "rowCount", rowCount, "sizeBytes", sizeBytes);
    }
}
