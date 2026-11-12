package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirSnapshotRetryWorker {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotRetryOne retryOne;
    private final TrassirMetrics metrics;

    /**
     * FIX #137: тот же паттерн, что в TrassirEventRetryWorker — без outer tx.
     * Короткий read + per-snapshot REQUIRES_NEW с advisory lock.
     */
    @Scheduled(fixedDelayString = "${pulse.trassir.snapshot-retry.interval-ms:60000}")
    public void retryFailedSnapshots() {
        long pending = snapshotRepository.countByStatus("PENDING");
        metrics.recordPendingSnapshots(pending);

        var candidates = snapshotRepository.findFailedForRetry(Instant.now());
        for (var snapshot : candidates) {
            try {
                retryOne.retry(snapshot.getId());
            } catch (Exception e) {
                log.warn("Snapshot retry threw for id={}", snapshot.getId(), e);
            }
        }
    }
}