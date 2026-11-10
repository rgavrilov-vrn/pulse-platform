package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventRetryWorker {

    private final TrassirEventRepository eventRepository;
    private final TrassirEventRouter router;
    private final ObjectMapper objectMapper;

    @Value("${pulse.trassir.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${pulse.trassir.retry.base-delay-seconds:30}")
    private long baseDelaySeconds;

    @Scheduled(fixedDelayString = "${pulse.trassir.retry.interval-ms:30000}")
    @Transactional
    public void retryFailedEvents() {
        List<TrassirEvent> events = eventRepository
                .findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                        "FAILED",
                        Instant.now()
                );

        for (TrassirEvent event : events) {
            try {
                EventProcessingResult result = router.route(toContext(event));

                if (result.successful()) {
                    event.setStatus("PROCESSED");
                    event.setProcessingError(null);
                    event.setLastError(null);
                    event.setNextRetryAt(null);
                } else {
                    markRetry(event, result.error());
                }
            } catch (Exception e) {
                log.warn("Retry failed for event {}: {}", event.getId(), e.getMessage(), e);
                markRetry(event, e.getClass().getSimpleName() + ": " + e.getMessage());
            }

            eventRepository.save(event);
        }
    }

    private TrassirEventContext toContext(TrassirEvent event) {
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

            return new TrassirEventContext(
                    event.getServerId(),
                    null,
                    Instant.now(),
                    parsed
            );
        } catch (Exception e) {
            throw new IllegalStateException("Cannot reconstruct event context", e);
        }
    }

    private void markRetry(TrassirEvent event, String error) {
        int nextAttempt = event.getRetryCount() + 1;
        event.setRetryCount(nextAttempt);
        event.setProcessingError(error);
        event.setLastError(error);

        if (nextAttempt >= maxAttempts) {
            event.setStatus("DEAD_LETTER");
            event.setNextRetryAt(null);
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(
                    Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt))
            );
        }
    }
}
