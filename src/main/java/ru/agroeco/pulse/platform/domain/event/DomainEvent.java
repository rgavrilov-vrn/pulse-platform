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

    /** Снимок «после» для audit. По умолчанию — само событие. */
    default Object newDataSnapshot() {
        return this;
    }

    /**
     * Тип события для outbox_events.event_type (и роутинга в Kafka-топик).
     *
     * По умолчанию — AuditAction.name() (UPPER_SNAKE, как в audit_log).
     * Интеграционные события могут переопределить, если Kafka-контракт
     * исторически использует другой стиль (PascalCase, см. IncidentCreated).
     */
    default String outboxEventType() {
        return eventType().getValue();
    }

    /**
     * Payload для outbox/Kafka.
     *
     * По умолчанию — само событие. Интеграционные события могут вернуть
     * обогащённый DTO, если domain-событие беднее, чем нужно consumer'ам.
     */
    default Object outboxPayload() {
        return this;
    }

    /** Публиковать ли в outbox. Ручной аудит и auth-события — нет. */
    default boolean publishToOutbox() {
        return true;
    }
}