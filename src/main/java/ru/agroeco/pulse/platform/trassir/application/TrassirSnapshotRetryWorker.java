package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirSnapshotRetryWorker {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirMetrics metrics;

    @Value("${pulse.trassir.snapshot-retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${pulse.trassir.snapshot-retry.base-delay-seconds:60}")
    private long baseDelaySeconds;

    @Scheduled(fixedDelayString = "${pulse.trassir.snapshot-retry.interval-ms:60000}")
    @Transactional
    public void retryFailedSnapshots() {
        long pending = snapshotRepository.countByStatus("PENDING");
        metrics.recordPendingSnapshots(pending);

        List<TrassirSnapshot> snapshots = snapshotRepository
                .findTop50ByStatusAndNextRetryAtBeforeOrderByNextRetryAtAsc(
                        "FAILED",
                        Instant.now()
                );

        for (TrassirSnapshot snapshot : snapshots) {
            metrics.incrementSnapshotRetried();

            int nextAttempt = snapshot.getRetryCount() + 1;
            snapshot.setRetryCount(nextAttempt);

            if (nextAttempt >= maxAttempts) {
                snapshotRepository.markDeadLetter(snapshot.getId());
                metrics.incrementSnapshotDeadLetter();
            } else {
                snapshotRepository.markFailed(
                        snapshot.getId(),
                        snapshot.getLastError(),
                        Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt))
                );
            }
        }
    }
}
