package ru.agroeco.pulse.platform.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    String eventType();

    Instant occurredAt();

    UUID entityId();

    String entitySchema();

    String entityTable();
}
