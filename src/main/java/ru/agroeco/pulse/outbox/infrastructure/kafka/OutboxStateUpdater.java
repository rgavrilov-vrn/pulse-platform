package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class OutboxStateUpdater {

    /**
     * Потолок экспоненциального backoff — 1 час.
     * Без него при maxAttempts > 20 получаем интервалы в годах.
     */
    private static final long MAX_BACKOFF_SECONDS = 3600L;

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

    /**
     * Идемпотентный no-op, если событие исчезло.
     * Раньше orElseThrow(IllegalStateException) обрывал весь батч в relay.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID id) {
        repository.findById(id).ifPresentOrElse(event -> {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            // managed — flush на коммите транзакции
        }, () -> log.warn("Outbox event {} not found on markPublished — skipping", id));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID id, String error) {
        repository.findById(id).ifPresentOrElse(event -> {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLastError(error);

            if (attempts >= properties.getMaxAttempts()) {
                event.setStatus(OutboxStatus.DEAD);
            } else {
                long backoffSeconds = Math.min(
                        MAX_BACKOFF_SECONDS,
                        (long) Math.pow(2, attempts));
                event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
            }
        }, () -> log.warn("Outbox event {} not found on markFailed — skipping", id));
    }
}