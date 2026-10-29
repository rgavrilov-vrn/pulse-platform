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
 *   claimBatch()  → короткая транзакция (FOR UPDATE SKIP LOCKED), возвращает DTO
 *   send()        → вне транзакции
 *   markSent/Fail → отдельная REQUIRES_NEW транзакция на каждое событие
 *
 * Семантика: at-least-once delivery. Между claimBatch-коммитом (лок снят)
 * и markSent() событие считается "висящим" — другой инстанс может его
 * повторно захватить и опубликовать дубль. Consumers обязаны дедуплицировать
 * по eventId (ProcessedEventPort). Это осознанный компромисс, чтобы не держать
 * DB-локи на время сетевого вызова Kafka.
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
        List<ClaimedEvent> batch = state.claimBatch(
                Instant.now(),
                properties.getBatchSize()
        );

        for (ClaimedEvent event : batch) {
            publishAndMark(event);
        }
    }

    private void publishAndMark(ClaimedEvent event) {
        try {
            String topic = resolveTopic(event.eventType());
            String key = event.entityId() == null
                    ? event.id().toString()
                    : event.entityId().toString();

            kafkaTemplate
                    .send(topic, key, event.payload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            state.markSent(event.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state.markFailed(event.id(), "Interrupted");
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Kafka send failed: eventId={}, error={}",
                    event.id(), e.getMessage());
            state.markFailed(event.id(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (RuntimeException e) {
            // Например, ошибка в state.markSent или неожиданный сбой в KafkaTemplate
            log.error("Unexpected error publishing outbox event {}", event.id(), e);
            state.markFailed(event.id(), e.getClass().getSimpleName());
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