package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Публикует накопленные outbox-события в Kafka.
 *
 * Архитектурно:
 *   claimBatch()    → короткая транзакция (FOR UPDATE SKIP LOCKED), возвращает DTO
 *   send()          → вне транзакции
 *   markPublished() → отдельная REQUIRES_NEW транзакция на каждое событие
 *
 * Семантика: at-least-once delivery. Между claimBatch-коммитом (лок снят)
 * и markPublished() событие считается "висящим" — другой инстанс может его
 * повторно захватить и опубликовать дубль. Consumers обязаны дедуплицировать
 * по eventId (ProcessedEventPort).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.outbox.relay-enabled", havingValue = "true")
@EnableConfigurationProperties(OutboxKafkaProperties.class)
public class OutboxKafkaRelay {

    private static final long SEND_TIMEOUT_SECONDS = 10L;

    private final OutboxStateUpdater state;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxKafkaProperties properties;

    public OutboxKafkaRelay(
            OutboxStateUpdater state,
            KafkaTemplate<String, String> kafkaTemplate,
            OutboxKafkaProperties properties
    ) {
        this.state = state;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:5000}")
    public void relayPendingEvents() {
        List<ClaimedEvent> batch;
        try {
            batch = state.claimBatch(Instant.now(), properties.getBatchSize());
        } catch (RuntimeException ex) {
            // Ошибка claim — весь цикл пропускаем, следующий запуск подхватит.
            log.error("Outbox claimBatch failed", ex);
            return;
        }

        for (ClaimedEvent event : batch) {
            if (!publishAndMark(event)) {
                // Interrupted — прерываем батч, следующие события заберёт следующий запуск.
                break;
            }
        }
    }

    /**
     * @return false, если прерваны (interrupt) — сигнал прервать батч.
     */
    private boolean publishAndMark(ClaimedEvent event) {
        try {
            String topic = resolveTopic(event.eventType());
            String key = event.entityId() == null
                    ? event.id().toString()
                    : event.entityId().toString();

            kafkaTemplate
                    .send(topic, key, event.payload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            safeMarkPublished(event.id());
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            safeMarkFailed(event.id(), "Interrupted");
            return false;   // прерываем for-цикл — поток всё равно не может продолжать

        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka send failed: eventId={}, error={}",
                    event.id(), e.getMessage());
            safeMarkFailed(event.id(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            return true;

        } catch (RuntimeException e) {
            log.error("Unexpected error publishing outbox event {}", event.id(), e);
            safeMarkFailed(event.id(), e.getClass().getSimpleName());
            return true;
        }
    }

    /**
     * markPublished/markFailed сами по себе REQUIRES_NEW и внутри ловят
     * "событие исчезло". Здесь дополнительный try/catch — на случай ошибки
     * БД, чтобы не обрывать батч из-за одного сбоя.
     */
    private void safeMarkPublished(java.util.UUID id) {
        try {
            state.markPublished(id);
        } catch (RuntimeException ex) {
            log.error("markPublished failed for event {}", id, ex);
        }
    }

    private void safeMarkFailed(java.util.UUID id, String error) {
        try {
            state.markFailed(id, error);
        } catch (RuntimeException ex) {
            log.error("markFailed failed for event {}", id, ex);
        }
    }

    private String resolveTopic(String eventType) {
        if (eventType == null) {
            return properties.getDefaultTopic();
        }
        return properties.getEventTopics()
                .getOrDefault(eventType, properties.getDefaultTopic());
    }
}