package ru.agroeco.pulse.platform.domain.event;

import lombok.Builder;
import lombok.With;
import ru.agroeco.pulse.audit.domain.event.AuditAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
@With
public record UserAuthenticationFailed(
    String username,
    String reason,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public AuditAction eventType() {
        return AuditAction.USER_AUTHENTICATION_FAILED;
    }

    @Override
    public UUID entityId() {
        return null;
    }

    @Override
    public String entitySchema() {
        return "auth";
    }

    @Override
    public String entityTable() {
        return "users";
    }

    @Override
    public Object newDataSnapshot() {
        // фиксируем только username и причину, без лишнего
        return Map.of("username", username, "reason", reason);
    }
}
