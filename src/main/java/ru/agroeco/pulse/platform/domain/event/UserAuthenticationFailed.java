package ru.agroeco.pulse.platform.domain.event;

import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Builder(toBuilder = true)
@With
public record UserAuthenticationFailed(
        String username,
        String reason,
        Instant occurredAt
) implements DomainEvent {

    public UserAuthenticationFailed {}

    @Override
    public DomainEventType eventType() {
        return DomainEventType.USER_AUTHENTICATION_FAILED;
    }

    @Override
    public java.util.UUID entityId() {
        // Нет сущности, к которой привязать событие — только username.
        // Требует миграции audit.audit_log.entity_id DROP NOT NULL (партия 2).
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
        // HashMap — чтобы избежать NPE, если reason когда-то будет null
        // (Map.of бросает NPE на null-значения).
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("reason", reason);
        return data;
    }

    @Override
    public boolean publishToOutbox() {
        // Auth-события не идут в Kafka (только audit).
        return false;
    }
}