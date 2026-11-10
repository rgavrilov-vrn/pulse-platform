package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.agroeco.pulse.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventRetryWorker {

    private final TrassirEventRepository eventRepository;
    private final TrassirEventProcessor eventProcessor;
    private final TrassirMetrics metrics;

    @Value("${pulse.trassir.retry.stale-pending-minutes:5}")
    private long stalePendingMinutes;

    @Value("${pulse.trassir.retry.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${pulse.trassir.retry.interval-ms:30000}")
    public void sweep() {
        Instant now = Instant.now();
        Instant staleCutoff = now.minusSeconds(stalePendingMinutes * 60);

        List<UUID> candidates = eventRepository.findCandidateIdsForRetry(
                now, staleCutoff, batchSize);

        for (UUID id : candidates) {
            try {
                // REQUIRES_NEW внутри, со SKIP LOCKED.
                eventProcessor.processById(id);
            } catch (Exception e) {
                log.warn("Event retry threw for id={}: {}", id, e.getMessage());
            }
        }
    }
}