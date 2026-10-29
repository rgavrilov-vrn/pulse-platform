package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventEntity;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;
import ru.agroeco.pulse.platform.application.outbox.OutboxStatus;

/**
 * Изолирует транзакционные операции над outbox_events.
 * Отдельный бин — чтобы Spring AOP-прокси реально применял @Transactional
 * (self-invocation внутри OutboxKafkaRelay не сработал бы).
 */
@Component
public class OutboxStateUpdater {

    private final OutboxEventRepository repository;
    private final OutboxKafkaProperties properties;

    public OutboxStateUpdater(OutboxEventRepository repository,
                              OutboxKafkaProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Короткая транзакция: SELECT ... FOR UPDATE SKIP LOCKED + маппинг в DTO.
     * Лок держится только на время этого SELECT, не на время Kafka send.
     */
    @Transactional
    public List<ClaimedEvent> claimBatch(Instant now, int batchSize) {
        return repository.lockPending(now, batchSize).stream()
                .map(e -> new ClaimedEvent(
                        e.getId(),
                        e.getEventType(),
                        e.getEntityId(),
                        e.getPayload()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID id) {
        OutboxEventEntity event = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event disappeared: " + id));
        event.setStatus(OutboxStatus.SENT);
        event.setPublishedAt(Instant.now());
        event.setLastError(null);
        // managed — flush произойдёт на коммите транзакции
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, String error) {
        OutboxEventEntity event = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event disappeared: " + id));

        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(error);

        if (attempts >= properties.getMaxAttempts()) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            long backoffSeconds = (long) Math.pow(2, attempts);
            event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
        }
    }
}