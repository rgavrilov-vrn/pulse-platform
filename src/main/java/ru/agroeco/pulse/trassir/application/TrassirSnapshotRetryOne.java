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
import java.util.Optional;
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

    /**
     * Один retry-цикл для конкретного snapshot.
     *
     * <p>FIX #29: assembly вызывается заново — ранее «ретрай» только сдвигал
     * {@code next_retry_at} без повторной обработки.</p>
     *
     * <p>FIX #77: SKIP LOCKED на lockByIdForRetry — параллельные инстансы
     * не дублируют работу.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(UUID snapshotId) {
        Instant now = Instant.now();

        // FIX #77: блокируем строку с SKIP LOCKED — если занято или уже
        // не FAILED/completed, выходим без работы.
        Optional<TrassirSnapshot> locked = snapshotRepository.lockByIdForRetry(snapshotId, now);
        if (locked.isEmpty()) {
            log.debug("Snapshot retry skipped (already processed or locked): id={}", snapshotId);
            return;
        }

        metrics.incrementSnapshotRetried();

        EventProcessingResult result = snapshotUseCase.assemble(snapshotId);
        if (result.successful()) {
            return;
        }

        // Снапшот уже managed — актуализируем поля из БД перед инкрементом.
        TrassirSnapshot snapshot = snapshotRepository.findById(snapshotId).orElse(null);
        if (snapshot == null || snapshot.getCompletedAt() != null) {
            return;
        }

        int nextAttempt = snapshot.getRetryCount() + 1;
        if (nextAttempt >= maxAttempts) {
            snapshotRepository.markDeadLetter(snapshotId);
            metrics.incrementSnapshotDeadLetter();
            log.error("Snapshot DEAD_LETTER after {} attempts: id={}, lastError={}",
                    nextAttempt, snapshotId, result.error());
        } else {
            snapshotRepository.markFailed(
                    snapshotId, result.error(),
                    Instant.now().plusSeconds(baseDelaySeconds * (1L << nextAttempt)));
        }
    }
}