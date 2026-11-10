package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventProcessor {

    private final TrassirEventRepository eventRepository;
    private final TrassirEventRouter router;
    private final ObjectMapper objectMapper;

    @ApplicationModuleListener
    public void onEventsAccepted(TrassirEventsAcceptedEvent accepted) {
        for (UUID eventId : accepted.eventIds()) {
            try {
                processEvent(eventId, accepted);
            } catch (Exception e) {
                log.warn("Async processing failed for event {}: {}", eventId, e.getMessage(), e);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(UUID eventId, TrassirEventsAcceptedEvent accepted) {
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null || "PROCESSED".equals(event.getStatus())) {
            return;
        }

        try {
            JsonNode source = objectMapper.readTree(event.getSource());
            JsonNode data = objectMapper.readTree(event.getData());

            ParsedTrassirEnvelope.Event parsed = new ParsedTrassirEnvelope.Event(
                    event.getEventId(),
                    event.getEventType(),
                    event.getEventVersion(),
                    event.getOccurredAt(),
                    event.getObservedAt(),
                    source,
                    data
            );

            TrassirEventContext context = new TrassirEventContext(
                    event.getServerId(),
                    accepted.batchId(),
                    accepted.receivedAt(),
                    parsed
            );

            EventProcessingResult result = router.route(context);

            if (result.successful()) {
                event.setStatus("PROCESSED");
                event.setProcessingError(null);
                event.setLastError(null);
                event.setNextRetryAt(null);
            } else {
                event.setStatus("FAILED");
                event.setProcessingError(result.error());
                event.setLastError(result.error());
                event.setRetryCount(0);
                event.setNextRetryAt(Instant.now());
            }
        } catch (Exception e) {
            event.setStatus("FAILED");
            event.setProcessingError(e.getClass().getSimpleName() + ": " + e.getMessage());
            event.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());
            event.setRetryCount(0);
            event.setNextRetryAt(Instant.now());
        }

        eventRepository.save(event);
    }
}
