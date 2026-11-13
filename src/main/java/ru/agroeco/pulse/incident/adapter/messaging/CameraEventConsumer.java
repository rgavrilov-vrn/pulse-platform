package ru.agroeco.pulse.incident.adapter.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.application.CameraIncidentUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.videoserver.domain.event.CameraOffline;
import ru.agroeco.pulse.videoserver.domain.event.CameraOnline;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Консьюмер derived camera-событий из pulse.videoserver.events.
 * Распознаёт по Kafka-header eventType: CAMERA_OFFLINE / CAMERA_ONLINE.
 * Остальные типы — skip.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CameraEventConsumer {

    public static final String CONSUMER_NAME = "incident-camera-event-consumer";
    private static final String HEADER_EVENT_TYPE = "eventType";

    private final CameraIncidentUseCase useCase;
    private final ProcessedEventPort processedEventPort;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topic.video-events}",
            groupId = "${app.kafka.consumer.group-id:pulse-incident-camera}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader(HEADER_EVENT_TYPE);
        String eventType = header == null ? null
                : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
        if (eventType == null) return;

        try {
            switch (eventType) {
                case "CAMERA_OFFLINE" -> handleOffline(record.value());
                case "CAMERA_ONLINE"  -> handleOnline(record.value());
                default -> { /* skip: MOTION, STATUS_CHANGED, ... */ }
            }
        } catch (JacksonException e) {
            log.error("Failed to deserialize {}: payloadLen={}", eventType,
                    record.value() == null ? 0 : record.value().length(), e);
        }
    }

    private void handleOffline(String payload) throws JacksonException {
        CameraOffline event = objectMapper.readValue(payload, CameraOffline.class);
        if (!processedEventPort.tryMarkProcessed(CONSUMER_NAME + ":offline", event.cameraId())) {
            return;
        }
        useCase.onCameraOffline(event);
    }

    private void handleOnline(String payload) throws JacksonException {
        CameraOnline event = objectMapper.readValue(payload, CameraOnline.class);
        if (!processedEventPort.tryMarkProcessed(CONSUMER_NAME + ":online", event.cameraId())) {
            return;
        }
        useCase.onCameraOnline(event);
    }
}