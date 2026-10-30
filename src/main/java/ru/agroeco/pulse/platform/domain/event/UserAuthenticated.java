package ru.agroeco.pulse.platform.domain.event;

import lombok.Builder;
import lombok.With;
import ru.agroeco.pulse.audit.domain.event.AuditAction;

import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true)
@With
public record UserAuthenticated(
        UUID userId,
        String username,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public AuditAction eventType() {
        return AuditAction.USER_AUTHENTICATED;
    }

    @Override
    public UUID entityId() {
        return userId;
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
    public UUID actorId() {
        // при логине SecurityContext может быть ещё не заполнен
        return userId;
    }

    @Override
    public boolean publishToOutbox() {
        // Auth-события пишутся только в audit, в Kafka не уходят.
        // Без этого флага OutboxListener пишет их в outbox_events,
        // где для UserAuthenticationFailed entity_id NOT NULL → падение.
        return false;
    }
}