package ru.agroeco.pulse.incidentorchestrator.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.api.IncidentCreated;
import ru.agroeco.pulse.incidentorchestrator.application.RouteIncidentUseCase;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;

@Component
public class IncidentCreatedConsumer {

    public static final String CONSUMER_NAME = "incident-orchestrator-consumer";

    private static final Logger log = LoggerFactory.getLogger(IncidentCreatedConsumer.class);

    private final RouteIncidentUseCase routeIncident;
    private final ProcessedEventPort processedEventPort;
    private final ObjectMapper objectMapper;

    public IncidentCreatedConsumer(
        RouteIncidentUseCase routeIncident,
        ProcessedEventPort processedEventPort,
        ObjectMapper objectMapper
    ) {
        this.routeIncident = routeIncident;
        this.processedEventPort = processedEventPort;
        this.objectMapper = objectMapper;
    }

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
        } catch (Exception exception) {
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
