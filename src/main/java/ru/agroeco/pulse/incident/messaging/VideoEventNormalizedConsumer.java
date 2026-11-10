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

    /**
     * FIX #56: topic/groupId из конфига.
     * FIX #55: при RuntimeException Spring Kafka через DefaultErrorHandler
     * (см. KafkaConsumerConfig) отправит сообщение в `<topic>.dlt` после
     * исчерпания ретраев. Раньше exception глотался и сообщение терялось.
     */
    @KafkaListener(
            topics = "${app.kafka.topic.video-events}",
            groupId = "${app.kafka.consumer.group-id:pulse-incident}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onVideoEventNormalized(String payload) {
        VideoEventNormalized event;
        try {
            event = objectMapper.readValue(payload, VideoEventNormalized.class);
        } catch (JacksonException exception) {
            // Не пробрасываем: битое сообщение — не retryable.
            log.error("Cannot deserialize VideoEventNormalized, dropping message. payload.length={}",
                    payload.length());
            return;
        }

        // tryMarkProcessed должен работать в той же транзакции (не REQUIRES_NEW),
        // иначе маркер «обработано» переживёт откат handle().
        if (!processedEventPort.tryMarkProcessed(CONSUMER_NAME, event.eventId())) {
            log.info("Skipping already processed video event {}", event.eventId());
            return;
        }

        // FIX #58: Optional.empty() — дубликат, уже создан (constraint hit).
        // FIX #55: RuntimeException → Spring Kafka → DLT.
        createIncident.handle(event).ifPresentOrElse(
                created -> log.debug("Incident {} created from event {}",
                        created.number(), event.eventId()),
                () -> log.info("Duplicate video event {} — incident already exists",
                        event.eventId()));
    }
}