package ru.agroeco.pulse.platform.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.outbox.relay-enabled", havingValue = "true")
public class OutboxRelay {
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final String topic;
    private final int batchSize;
    private final int maxAttempts;
    private final long timeoutSeconds;

    public OutboxRelay(OutboxEventRepository repository, KafkaTemplate<String, String> kafka, ObjectMapper mapper,
                       @Value("${app.kafka.topic.incident-events}") String topic,
                       @Value("${app.outbox.batch-size:50}") int batchSize,
                       @Value("${app.outbox.max-attempts:5}") int maxAttempts,
                       @Value("${app.outbox.send-timeout-seconds:10}") long timeoutSeconds) {
        if (batchSize < 1 || batchSize > 500 || maxAttempts < 1 || timeoutSeconds < 1)
            throw new IllegalArgumentException("Invalid outbox configuration");
        this.repository = repository;
        this.kafka = kafka;
        this.mapper = mapper;
        this.topic = topic;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.timeoutSeconds = timeoutSeconds;
    }

    // Deliberately retains transaction-scoped locks while sending; no lease protocol yet.
    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        var pending = repository.lockPending(Instant.now(), batchSize);
        for (var event : pending) {
            try {
                var envelope = mapper.createObjectNode();
                envelope.put("eventId", event.getId().toString());
                envelope.put("aggregateId", event.getEntityId().toString());
                envelope.put("eventType", event.getEventType());
                envelope.put("schemaVersion", 1);
                envelope.put("recordedAt", event.getCreatedAt().toString());
                envelope.set("payload", mapper.readTree(event.getPayload()));
                var message = MessageBuilder.withPayload(mapper.writeValueAsString(envelope))
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, event.getEntityId().toString()).build();
                kafka.send(message).get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Relay interrupted", ex);
                }
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setLastError(ex.getClass().getSimpleName());
                event.setNextAttemptAt(Instant.now().plusSeconds(1L << Math.min(attempts, 8)));
                if (attempts >= maxAttempts) event.setStatus(OutboxStatus.FAILED);
                log.warn("Outbox publication failed: eventId={}", event.getId(), ex);
                continue;
            }
            event.setStatus(OutboxStatus.SENT);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
        }
        // Managed entities are flushed on commit; DB failures are not Kafka failures.
        repository.flush();
    }
}
