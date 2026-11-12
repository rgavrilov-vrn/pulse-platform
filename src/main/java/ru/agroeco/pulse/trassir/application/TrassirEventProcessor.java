package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import ru.agroeco.pulse.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.trassir.domain.TrassirIngestRaw;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.trassir.repository.TrassirIngestRawRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventProcessor {

    private final TrassirEventRepository eventRepository;
    private final TrassirIngestRawRepository rawRepository;    // FIX #136
    private final TrassirEventRouter router;
    private final TrassirMetrics metrics;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Value("${pulse.trassir.retry.max-attempts:5}") private int maxAttempts;
    @Value("${pulse.trassir.retry.base-delay-seconds:30}") private long baseDelaySeconds;

    @Async("trassirAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventsAccepted(TrassirEventsAcceptedEvent accepted) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (UUID eventId : accepted.eventIds()) {
            try {
                tx.executeWithoutResult(s -> processOne(eventId, accepted));
            } catch (Exception e) {
                log.warn("Async processing failed for event {}", eventId, e);
                try { tx.executeWithoutResult(s -> failWithRetry(eventId, e)); }
                catch (Exception nested) { log.error("Cannot mark event {} as FAILED", eventId, nested); }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processById(UUID eventId) {
        Boolean locked = eventRepository.tryAdvisoryLock(eventId.toString());
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("Event {} already being processed elsewhere, skipping", eventId);
            return;
        }
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null || "PROCESSED".equals(event.getStatus())) return;

        // FIX #136: восстанавливаем batchId и receivedAt из ingest_raw.
        // Раньше processById передавал null batchId и Instant.now() — handler'ы,
        // которым нужна оригинальная атрибуция, получали мусор.
        TrassirIngestRaw raw = rawRepository.findById(event.getRawId()).orElse(null);
        TrassirEventsAcceptedEvent ctx = new TrassirEventsAcceptedEvent(
                event.getRawId(),
                event.getServerId(),
                raw != null ? raw.getBatchId() : null,
                raw != null ? raw.getReceivedAt() : event.getObservedAt(),
                java.util.List.of(eventId));

        try {
            processOne(eventId, ctx);
        } catch (Exception e) {
            log.warn("Processing failed for event {}: {}", eventId, e.getMessage());
            failWithRetry(eventId, e);
        }
    }

    private void processOne(UUID eventId, TrassirEventsAcceptedEvent ctx) {
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null || "PROCESSED".equals(event.getStatus())) return;

        long startedAt = System.nanoTime();
        try {
            ParsedTrassirEnvelope.Event parsed = new ParsedTrassirEnvelope.Event(
                    event.getEventId(), event.getEventType(), event.getEventVersion(),
                    event.getOccurredAt(), event.getObservedAt(),
                    objectMapper.readTree(event.getSource()),
                    objectMapper.readTree(event.getData()));

            TrassirEventContext tc = new TrassirEventContext(
                    event.getServerId(), ctx.batchId(), ctx.receivedAt(), parsed);

            EventProcessingResult result = router.route(tc);
            if (!result.successful()) {
                failEventInTx(event, result.error());
                return;
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

    private void failWithRetry(UUID eventId, Exception cause) {
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null || "PROCESSED".equals(event.getStatus())) return;
        failEventInTx(event, cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private void failEventInTx(TrassirEvent event, String error) {
        int nextAttempt = event.getRetryCount() + 1;
        event.setRetryCount(nextAttempt);
        if (nextAttempt >= maxAttempts) {
            event.setStatus("DEAD_LETTER");
            event.setNextRetryAt(null);
            metrics.incrementEventDeadLetter(event.getEventType());
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt)));
        }
        event.setProcessingError(error);
        event.setLastError(error);
        eventRepository.saveAndFlush(event);
        metrics.incrementEventFailed(event.getEventType(), classify(error));
    }

    private static String classify(String error) {
        if (error == null) return "UNKNOWN";
        String lower = error.toLowerCase();
        if (lower.contains("snapshot")) return "SNAPSHOT";
        if (lower.contains("parse") || lower.contains("json")) return "PARSE";
        if (lower.contains("constraint") || lower.contains("unique")) return "DB";
        return "PROCESSING";
    }
}