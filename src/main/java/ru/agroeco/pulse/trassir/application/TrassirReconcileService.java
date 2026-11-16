package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSnapshotObjectRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirSnapshotRepository;
import ru.agroeco.pulse.trassir.api.event.TrassirObjectsSnapshotReceived;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirReconcileService {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final EventDispatcher events;

    @Transactional
    public Map<String, Object> reconcileSnapshot(UUID snapshotId) {
        TrassirSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new NotFoundException("Snapshot not found: " + snapshotId));
        if (snapshot.getCompletedAt() == null) {
            throw new IllegalStateException("Snapshot not completed yet: " + snapshotId);
        }
        return doReconcile(snapshot);
    }

    @Transactional
    public Map<String, Object> reconcileLatestCompletedSnapshot(UUID serverId) {
        // ✅ derived-method (без JPQL с ? IS NULL — избегаем $N type inference)
        TrassirSnapshot snapshot = snapshotRepository
                .findFirstByServerIdAndStatusOrderByCompletedAtDesc(serverId, "COMPLETED")
                .orElseThrow(() -> new NotFoundException(
                        "No completed snapshot for server " + serverId));
        return doReconcile(snapshot);
    }

    private Map<String, Object> doReconcile(TrassirSnapshot snapshot) {
        List<TrassirObjectsSnapshotReceived.SnapshotObject> objects =
                snapshotObjectRepository.findBySnapshotId(snapshot.getId()).stream()
                        .map(o -> new TrassirObjectsSnapshotReceived.SnapshotObject(
                                o.getObjectKey(), o.getObjectPayload()))
                        .toList();

        Instant completedAt = snapshot.getCompletedAt();
        TrassirObjectsSnapshotReceived event = new TrassirObjectsSnapshotReceived(
                snapshot.getId(),
                1,
                snapshot.getServerId(),
                snapshot.getId(),
                completedAt,
                objects,
                CorrelationIds.current());

        events.dispatch(event);
        log.info("Re-dispatch TrassirObjectsSnapshotReceived: snapshotId={}, serverId={}, objects={}",
                snapshot.getId(), snapshot.getServerId(), objects.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snapshotId", snapshot.getId());
        result.put("serverId", snapshot.getServerId());
        result.put("objectsDispatched", objects.size());
        result.put("completedAt", completedAt);
        return result;
    }
}