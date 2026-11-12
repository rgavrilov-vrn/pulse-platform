package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirEventRepository;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventRetryWorker {

    private final TrassirEventRepository eventRepository;
    private final TrassirEventProcessor eventProcessor;
    private final TrassirMetrics metrics;

    @Value("${pulse.trassir.retry.stale-pending-minutes:5}")
    private long stalePendingMinutes;

    /**
     * FIX #137: без @Transactional на методе. Outer-транзакция с FOR UPDATE
     * + REQUIRES_NEW внутри давала deadlock. Теперь:
     *   1. короткий read findProcessableCandidates (без блокировок);
     *   2. каждая event обрабатывается отдельно (processById — REQUIRES_NEW
     *      + advisory lock).
     */
    @Scheduled(fixedDelayString = "${pulse.trassir.retry.interval-ms:30000}")
    public void sweep() {
        Instant now = Instant.now();
        Instant staleCutoff = now.minusSeconds(stalePendingMinutes * 60);
        var candidates = eventRepository.findProcessableCandidates(now, staleCutoff);

        for (TrassirEvent event : candidates) {
            if ("PENDING".equals(event.getStatus())) {
                log.warn("Stale PENDING event picked up: id={}, created={}",
                        event.getId(), event.getCreatedAt());
            }
            metrics.incrementEventRetried(event.getEventType());
            try {
                eventProcessor.processById(event.getId());
            } catch (Exception e) {
                log.warn("Retry threw for event {}: {}", event.getId(), e.getMessage());
            }
        }
    }
}