package ru.agroeco.pulse.refs.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.audit.domain.event.AuditAction;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public record FacilityCreated(
        UUID facilityId,
        String code,
        String name,
        UUID actorId,
        Instant occurredAt
) implements DomainEvent {

    @Override public AuditAction eventType() { return AuditAction.FACILITY_CREATED; }
    @Override public UUID entityId() { return facilityId; }
    @Override public String entitySchema() { return "ref"; }
    @Override public String entityTable() { return "objects"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }

    @Override
    public boolean publishToOutbox() {
        // refs-события не идут в Kafka — только в audit_log.
        return false;
    }

    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("name", name);
        return data;
    }
}