package ru.agroeco.pulse.platform.domain.event;

import java.util.UUID;

/**
 * Межмодульное интеграционное событие. Сейчас передаётся через EventDispatcher
 * (in-process), в будущем — через Kafka без изменения контракта.
 *
 * <p>Отличия от {@link DomainEvent}: обязательные {@code eventId} (для consumer-side
 * dedup через {@code outbox.processed_events}) и {@code schemaVersion} (для evolution).</p>
 */
public interface IntegrationEvent extends DomainEvent {

    /** Стабильный ID события для дедупликации на стороне потребителя. */
    UUID eventId();

    /** Версия схемы события для future evolution. */
    int schemaVersion();
}