package ru.agroeco.pulse.trassir.application;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirEventRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirIngestRawRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSnapshotRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSystemEventRepository;
import ru.agroeco.pulse.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.trassir.domain.TrassirIngestRaw;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirAdminOperationsService {

    private final TrassirEventRepository eventRepository;
    private final TrassirIngestRawRepository rawRepository;
    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSystemEventRepository systemEventRepository;

    @Transactional
    public int retryEvent(UUID eventId) {
        int n = eventRepository.markForRetry(eventId, Instant.now());
        log.info("Admin retry event: id={}, affected={}", eventId, n);
        return n;
    }

    @Transactional
    public int retryEventsBulk(UUID serverId, String eventType, Instant from) {
        int n = eventRepository.markForRetryBulk(serverId, eventType, from, Instant.now());
        log.info("Admin bulk retry events: serverId={}, eventType={}, from={}, affected={}",
                serverId, eventType, from, n);
        return n;
    }

    @Transactional
    public Map<String, Object> retrySnapshot(UUID snapshotId) {
        TrassirSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NotFoundException("Snapshot not found: " + snapshotId));
        if (snapshot.getCompletedAt() != null) {
            return Map.of(
                    "snapshotId", snapshotId,
                    "status", "already_completed",
                    "completedAt", snapshot.getCompletedAt());
        }
        snapshotRepository.markFailed(snapshotId, "Manually queued for retry", Instant.now());
        return Map.of(
                "snapshotId", snapshotId,
                "status", "queued_for_retry",
                "partsTotal", snapshot.getTotalParts());
    }

    @Transactional
    public Map<String, Object> deleteBatch(UUID batchId, boolean cascadeEvents) {
        TrassirIngestRaw raw = rawRepository.findByBatchId(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        int deletedEvents = 0;
        if (cascadeEvents) {
            deletedEvents = eventRepository.deleteByRawId(raw.getId());
        } else {
            // ✅ FIX: count(Specification) — теперь доступен через JpaSpecificationExecutor
            long active = eventRepository.count(byRawId(raw.getId()));
            if (active > 0) {
                throw new IllegalStateException(
                        "Batch has " + active + " events; pass cascadeEvents=true to delete");
            }
        }
        int deletedRaw = rawRepository.deleteByBatchId(batchId);
        log.warn("Admin delete batch: batchId={}, deletedEvents={}, deletedRaw={}",
                batchId, deletedEvents, deletedRaw);

        return Map.of(
                "batchId", batchId,
                "deletedEvents", deletedEvents,
                "deletedRaw", deletedRaw);
    }

    @Transactional
    public int purgeSystemEvents(Instant before) {
        int n = systemEventRepository.deleteOlderThan(before);
        log.warn("Admin purge system_events before={}, deleted={}", before, n);
        return n;
    }

    @Transactional
    public int purgeFinishedEvents(Instant before) {
        int n = eventRepository.deleteFinishedOlderThan(before);
        log.warn("Admin purge finished events before={}, deleted={}", before, n);
        return n;
    }

    // ---------------------------------------------------------------- specs

    private static Specification<TrassirEvent> byRawId(UUID rawId) {
        return (root, query, cb) -> {
            Predicate p = cb.equal(root.get("rawId"), rawId);
            return p;
        };
    }
}