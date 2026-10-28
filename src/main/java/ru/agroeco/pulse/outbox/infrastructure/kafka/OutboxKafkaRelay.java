package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.domain.OutboxEvent;
import ru.agroeco.pulse.outbox.infrastructure.persistence.OutboxEventRepository;

@Component
public class OutboxKafkaRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxKafkaRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxKafkaProperties properties;

    public OutboxKafkaRelay(
        OutboxEventRepository repository,
        KafkaTemplate<String, String> kafkaTemplate,
        OutboxKafkaProperties properties
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${pulse.outbox.relay-interval-ms:5000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> events = repository
            .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                OutboxEvent.Status.NEW.name(),
                Instant.now()
            );

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            String topic = resolveTopic(event.getEventType());
            kafkaTemplate.send(topic, event.getEntityId().toString(), event.getPayload())
                .get(10, TimeUnit.SECONDS);

            event.markSent(Instant.now());
            repository.save(event);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail(event, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        } catch (ExecutionException | TimeoutException exception) {
            fail(event, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private void fail(OutboxEvent event, String error) {
        log.warn("Failed to publish outbox event {}: {}", event.getId(), error);
        event.markFailed(properties.getMaxAttempts(), Instant.now(), error);
        repository.save(event);
    }

    String resolveTopic(String eventType) {
        if (eventType == null) {
            return properties.getDefaultTopic();
        }

        return switch (eventType) {
            case "VideoEventNormalized" -> "pulse.videoserver.events";
            case "IncidentCreated",
                 "IncidentAssigned",
                 "IncidentConfirmed",
                 "IncidentRejected",
                 "IncidentClosed" -> "pulse.incident.events";
            case "NotificationRequested" -> "pulse.notification.requests";
            case "NotificationStatus" -> "pulse.notification.status";
            default -> properties.getDefaultTopic();
        };
    }
}
