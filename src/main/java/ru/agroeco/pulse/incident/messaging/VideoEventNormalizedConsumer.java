package ru.agroeco.pulse.incident.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.application.CreateIncidentFromVideoEventUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoEventNormalizedConsumer {

    public static final String CONSUMER_NAME = "incident-video-event-consumer";

    private final CreateIncidentFromVideoEventUseCase createIncident;
    private final ProcessedEventPort processedEventPort;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "pulse.videoserver.events",
        groupId = "pulse-incident",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onVideoEventNormalized(String payload) {
        VideoEventNormalized event;
        try {
            event = objectMapper.readValue(payload, VideoEventNormalized.class);
        } catch (JacksonException exception) {
            log.error("Failed to deserialize VideoEventNormalized: {}", payload, exception);
            return;
        }

        if (!processedEventPort.tryMarkProcessed(CONSUMER_NAME, event.eventId())) {
            log.info("Skipping already processed video event {}", event.eventId());
            return;
        }

        createIncident.handle(event);
    }
}
