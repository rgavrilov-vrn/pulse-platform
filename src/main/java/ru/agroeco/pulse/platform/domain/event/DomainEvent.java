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

    default UUID actorId() { return null; }
    default Object oldDataSnapshot() { return null; }
    default Object newDataSnapshot() { return this; }

    /** Тип события для outbox_events.event_type (и роутинга в Kafka). */
    default String outboxEventType() { return eventType().getValue(); }

    /** Payload для outbox/Kafka. По умолчанию — само событие. */
    default Object outboxPayload() { return this; }

    /** Публиковать ли в outbox. Auth/audit-only события — нет. */
    default boolean publishToOutbox() { return true; }
}