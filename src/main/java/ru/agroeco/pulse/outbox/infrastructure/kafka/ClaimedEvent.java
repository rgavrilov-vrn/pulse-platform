package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.util.UUID;

/**
 * Снапшот outbox-события на момент захвата батча.
 * Отдаётся наружу из транзакции вместо JPA-entity, чтобы не тащить detached-сущность
 * и не зависеть от lazy-полей после коммита claim-транзакции.
 */
public record ClaimedEvent(
        UUID id,
        String eventType,
        UUID entityId,
        String payload
) {
}