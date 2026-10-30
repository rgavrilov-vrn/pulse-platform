package ru.agroeco.pulse.incidentorchestrator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.domain.event.IncidentCreated;
import ru.agroeco.pulse.incidentorchestrator.application.RouteIncidentUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentCreatedConsumer {

    public static final String CONSUMER_NAME = "incident-orchestrator-consumer";

    private final RouteIncidentUseCase routeIncident;
    private final ProcessedEventPort processedEventPort;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "pulse.incident.events",
        groupId = "pulse-incident-orchestrator",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onIncidentCreated(String payload) {
        IncidentCreated event;
        try {
            event = objectMapper.readValue(payload, IncidentCreated.class);
        } catch (JacksonException exception) {
            log.error("Failed to deserialize IncidentCreated: {}", payload, exception);
            return;
        }

        if (!processedEventPort.tryMarkProcessed(CONSUMER_NAME, event.incidentId())) {
            log.info("Skipping already processed incident {}", event.incidentId());
            return;
        }

        routeIncident.handle(event);
    }
}
