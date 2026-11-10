package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventProcessor {
    private final TrassirEventRepository eventRepository;
    private final TrassirEventRouter router;
    private final TrassirMetrics metrics;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventsAccepted(TrassirEventsAcceptedEvent accepted) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (UUID eventId : accepted.eventIds()) {
            try {
                transaction.executeWithoutResult(status -> processEvent(eventId, accepted));
            } catch (Exception e) {
                log.warn("Async processing failed for event {}", eventId, e);
                transaction.executeWithoutResult(status -> {
                    TrassirEvent event = eventRepository.findById(eventId).orElse(null);
                    if (event != null && !"PROCESSED".equals(event.getStatus())) {
                        fail(event, e.getClass().getSimpleName() + ": " + e.getMessage());
                        eventRepository.save(event);
                    }
                });
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEvent(UUID eventId, TrassirEventsAcceptedEvent accepted) {
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null || "PROCESSED".equals(event.getStatus())) return;
        long startedAt = System.nanoTime();
        try {
            ParsedTrassirEnvelope.Event parsed = new ParsedTrassirEnvelope.Event(
                    event.getEventId(), event.getEventType(), event.getEventVersion(),
                    event.getOccurredAt(), event.getObservedAt(),
                    objectMapper.readTree(event.getSource()), objectMapper.readTree(event.getData()));
            EventProcessingResult result = router.route(new TrassirEventContext(
                    event.getServerId(), accepted.batchId(), accepted.receivedAt(), parsed));
            if (!result.successful()) {
                throw new IllegalStateException(result.error());
            }
            event.setStatus("PROCESSED");
            event.setProcessingError(null);
            event.setLastError(null);
            event.setNextRetryAt(null);
            eventRepository.saveAndFlush(event);
            metrics.incrementEventProcessed(event.getEventType());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot process TRASSIR event", e);
        } finally {
            metrics.recordEventProcessing(event.getEventType(), System.nanoTime() - startedAt);
        }
    }

    private void fail(TrassirEvent event, String error) {
        event.setStatus("FAILED");
        event.setProcessingError(error);
        event.setLastError(error);
        event.setNextRetryAt(Instant.now());
        metrics.incrementEventFailed(event.getEventType(), error);
    }
}
