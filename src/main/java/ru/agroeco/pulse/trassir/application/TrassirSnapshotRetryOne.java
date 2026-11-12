package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirSnapshotRetryOne {

    private final TrassirSnapshotRepository snapshotRepository;
    private final ProcessTrassirObjectsSnapshotUseCase snapshotUseCase;
    private final TrassirMetrics metrics;

    @Value("${pulse.trassir.snapshot-retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${pulse.trassir.snapshot-retry.base-delay-seconds:60}")
    private long baseDelaySeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(UUID snapshotId) {
        metrics.incrementSnapshotRetried();

        EventProcessingResult result = snapshotUseCase.assemble(snapshotId);
        if (result.successful()) return;

        TrassirSnapshot snapshot = snapshotRepository.findById(snapshotId).orElse(null);
        if (snapshot == null || snapshot.getCompletedAt() != null) return;

        int nextAttempt = snapshot.getRetryCount() + 1;
        if (nextAttempt >= maxAttempts) {
            snapshotRepository.markDeadLetter(snapshotId);
            metrics.incrementSnapshotDeadLetter();
            log.error("Snapshot DEAD_LETTER after {} attempts: id={}, lastError={}",
                    nextAttempt, snapshotId, result.error());
        } else {
            snapshotRepository.markFailed(
                    snapshotId,
                    result.error(),
                    Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt)));
        }
    }
}