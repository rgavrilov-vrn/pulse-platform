package ru.agroeco.pulse.platform.domain.event;

import ru.agroeco.pulse.audit.domain.event.AuditAction;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    AuditAction eventType();

    UUID entityId();

    String entitySchema();

    String entityTable();

    Instant occurredAt();

    /** Актор. null → листенер возьмёт из SecurityContext. */
    default UUID actorId() {
        return null;
    }

    /** Снимок «до». null → в audit_log.old_data пишется null. */
    default Object oldDataSnapshot() {
        return null;
    }

    /** Снимок «после». По умолчанию — само событие. */
    default Object newDataSnapshot() {
        return this;
    }

    /** Публиковать ли в outbox. Ручной аудит — нет. */
    default boolean publishToOutbox() {
        return true;
    }
}
