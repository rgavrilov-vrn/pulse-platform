package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirSnapshotRetryWorker {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotRetryOne retryOne;
    private final TrassirMetrics metrics;

    @Value("${pulse.trassir.snapshot-retry.batch-size:50}")
    private int batchSize;

    /**
     * FIX #29: реальный retry через {@link TrassirSnapshotRetryOne#retry(UUID)}.
     * FIX #76, #77: каждый snapshot обрабатывается в своей REQUIRES_NEW
     * транзакции с SKIP LOCKED — параллельные инстансы не мешают,
     * падение одного не откатывает остальные.
     *
     * <p>Метод БЕЗ {@code @Transactional}: read candidate-ids не требует
     * транзакции, а per-snapshot работа изолирована в retryOne.</p>
     */
    @Scheduled(fixedDelayString = "${pulse.trassir.snapshot-retry.interval-ms:60000}")
    public void retryFailedSnapshots() {
        // Метрика pending — отдельный быстрый count, вне batch-выборки.
        long pending = snapshotRepository.countByStatus("PENDING");
        metrics.recordPendingSnapshots(pending);

        List<UUID> candidates = snapshotRepository
                .findCandidateIdsForRetry(Instant.now(), batchSize);

        for (UUID id : candidates) {
            try {
                retryOne.retry(id);
            } catch (Exception e) {
                log.warn("Snapshot retry threw for id={}", id, e);
            }
        }
    }
}