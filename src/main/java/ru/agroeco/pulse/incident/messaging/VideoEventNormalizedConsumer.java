package ru.agroeco.pulse.incident.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.application.CreateIncidentFromVideoEventUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

@Component
public class VideoEventNormalizedConsumer {

    public static final String CONSUMER_NAME = "incident-video-event-consumer";

    private static final Logger log = LoggerFactory.getLogger(VideoEventNormalizedConsumer.class);

    private final CreateIncidentFromVideoEventUseCase createIncident;
    private final ProcessedEventPort processedEventPort;
    private final ObjectMapper objectMapper;

    public VideoEventNormalizedConsumer(
        CreateIncidentFromVideoEventUseCase createIncident,
        ProcessedEventPort processedEventPort,
        ObjectMapper objectMapper
    ) {
        this.createIncident = createIncident;
        this.processedEventPort = processedEventPort;
        this.objectMapper = objectMapper;
    }

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
        } catch (Exception exception) {
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
