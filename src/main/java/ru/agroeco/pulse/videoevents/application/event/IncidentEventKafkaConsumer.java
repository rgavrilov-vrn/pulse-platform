package ru.agroeco.pulse.videoevents.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoevents.adapter.persistence.outbox.InboxEventRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class IncidentEventKafkaConsumer {
    private final ObjectMapper mapper;
    private final InboxEventRepository inbox;

    @KafkaListener(topics = "${app.kafka.topic.incident-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"})
    @Transactional
    public void on(String message) throws Exception {
        var event = mapper.readTree(message);
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        UUID aggregateId = UUID.fromString(event.path("aggregateId").asText());
        String type = event.path("eventType").asText();
        if (event.path("schemaVersion").asInt() != 1 || type.isBlank() || !event.has("payload"))
            throw new IllegalArgumentException("Invalid or unsupported envelope");
        if (inbox.insertIfAbsent("incident-log-v1", eventId) == 0) return;
        // Future DB side effects must participate in this same transaction.
        log.info("Kafka event handled: id={}, type={}, aggregateId={}", eventId, type, aggregateId);
    }
}
