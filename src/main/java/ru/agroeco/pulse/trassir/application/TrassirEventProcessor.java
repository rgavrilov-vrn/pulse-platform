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
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
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

    @Value("${pulse.trassir.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${pulse.trassir.retry.base-delay-seconds:30}")
    private long baseDelaySeconds;

    @Value("${pulse.trassir.retry.stale-pending-minutes:5}")
    private long stalePendingMinutes;

    // =========================================================================
    // 1. Основной путь: свежие события, принятые ingest'ом.
    //    Слушатель срабатывает ПОСЛЕ коммита внешней транзакции,
    //    конкуренции нет — блокировка не нужна.
    // =========================================================================
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventsAccepted(TrassirEventsAcceptedEvent accepted) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (UUID eventId : accepted.eventIds()) {
            try {
                tx.executeWithoutResult(s -> processOne(eventId, accepted.receivedAt()));
            } catch (Exception e) {
                log.warn("Async processing failed for event {}", eventId, e);
                try {
                    tx.executeWithoutResult(s -> failById(eventId, e));
                } catch (Exception nested) {
                    log.error("Cannot mark event {} as FAILED", eventId, nested);
                }
            }
        }
    }

    // =========================================================================
    // 2. Путь ретрая: вызывается TrassirEventRetryWorker'ом.
    //    Сам захватывает SKIP LOCKED — параллельные инстансы не пересекаются,
    //    и нет deadlock'а «outer держит lock → inner REQUIRES_NEW лочит то же».
    // =========================================================================
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processById(UUID eventId) {
        Instant now = Instant.now();
        Instant staleCutoff = now.minusSeconds(stalePendingMinutes * 60);

        TrassirEvent locked = eventRepository
                .lockByIdForRetry(eventId, now, staleCutoff)
                .orElse(null);

        if (locked == null) {
            // Кто-то другой уже забрал / статус изменился / next_retry_at ещё не наступил.
            log.debug("Event {} not lockable for retry — skipped", eventId);
            return;
        }

        metrics.incrementEventRetried(locked.getEventType());

        try {
            processOne(locked, now);
        } catch (Exception e) {
            log.warn("Retry failed for event {}: {}", eventId, e.getMessage());
            failWithRetry(locked, e);
        }
    }

    // =========================================================================
    // Внутренняя логика
    // =========================================================================

    private void processOne(UUID eventId, Instant receivedAt) {
        TrassirEvent event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return;
        processOne(event, receivedAt);
    }

    private void processOne(TrassirEvent event, Instant receivedAt) {
        if ("PROCESSED".equals(event.getStatus())) return;

        long startedAt = System.nanoTime();
        try {
            ParsedTrassirEnvelope.Event parsed = new ParsedTrassirEnvelope.Event(
                    event.getEventId(),
                    event.getEventType(),
                    event.getEventVersion(),
                    event.getOccurredAt(),
                    event.getObservedAt(),
                    objectMapper.readTree(event.getSource()),
                    objectMapper.readTree(event.getData()));

            TrassirEventContext ctx = new TrassirEventContext(
                    event.getServerId(), null, receivedAt, parsed);

            EventProcessingResult result = router.route(ctx);
            if (!result.successful()) {
                // FIX #16: НЕ бросаем — иначе markFailed откатится вместе с TX.
                applyFailure(event, result.error());
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

    private void failById(UUID eventId, Exception cause) {
        eventRepository.findById(eventId).ifPresent(e -> failWithRetry(e, cause));
    }

    private void failWithRetry(TrassirEvent event, Exception cause) {
        if ("PROCESSED".equals(event.getStatus())) return;
        String error = cause.getClass().getSimpleName()
                + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
        applyFailure(event, error);
    }

    private void applyFailure(TrassirEvent event, String error) {
        int nextAttempt = event.getRetryCount() + 1;
        event.setRetryCount(nextAttempt);

        if (nextAttempt >= maxAttempts) {
            event.setStatus("DEAD_LETTER");
            event.setNextRetryAt(null);
            metrics.incrementEventDeadLetter(event.getEventType());
            log.error("Event DEAD_LETTER after {} attempts: id={}, type={}, error={}",
                    nextAttempt, event.getId(), event.getEventType(), error);
        } else {
            event.setStatus("FAILED");
            event.setNextRetryAt(
                    Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt)));
        }

        event.setProcessingError(error);
        event.setLastError(error);
        eventRepository.saveAndFlush(event);
        metrics.incrementEventFailed(event.getEventType(), classify(error));
    }

    /** FIX #80: bounded-метка в метрике, без уникальных сообщений. */
    private static String classify(String error) {
        if (error == null) return "UNKNOWN";
        String lower = error.toLowerCase();
        if (lower.contains("snapshot")) return "SNAPSHOT";
        if (lower.contains("parse") || lower.contains("json")) return "PARSE";
        if (lower.contains("constraint") || lower.contains("unique")) return "DB";
        if (lower.contains("cannot process")) return "PROCESSING";
        return "OTHER";
    }
}