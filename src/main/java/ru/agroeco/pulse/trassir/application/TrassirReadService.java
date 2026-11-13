package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.trassir.adapter.persistence.*;
import ru.agroeco.pulse.trassir.application.dto.*;
import ru.agroeco.pulse.trassir.domain.TrassirServerState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrassirReadService {

    private final TrassirServerStateRepository serverStateRepository;
    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final TrassirSystemEventRepository systemEventRepository;
    private final TrassirEventRepository eventRepository;

    public List<TrassirServerView> listServers() {
        return serverStateRepository.findAll().stream()
                .map(this::toServerView)
                .toList();
    }

    public TrassirServerView getServer(UUID serverId) {
        TrassirServerState state = serverStateRepository.findById(serverId)
                .orElseThrow(() -> new NotFoundException("TRASSIR server state not found: " + serverId));
        return toServerView(state);
    }

    public PageResponse<TrassirSnapshotView> listSnapshots(UUID serverId, String status,
                                                           Instant from, Instant to, Pageable pageable) {
        Page<TrassirSnapshotView> page = snapshotRepository
                .search(serverId, status, from, to, pageable)
                .map(s -> new TrassirSnapshotView(
                        s.getId(), s.getExternalSnapshotId(),
                        s.getServerId() != null ? s.getServerId().toString() : null,
                        s.getStreamId(), s.getSnapshotSeq(), s.getTotalParts(),
                        s.getReceivedAt(), s.getCompletedAt(), s.getStatus(),
                        s.getRetryCount(), s.getLastError(), s.getNextRetryAt()));
        return PageResponse.of(page);
    }

    public List<TrassirSnapshotObjectView> listSnapshotObjects(UUID snapshotId) {
        return snapshotObjectRepository.findBySnapshotId(snapshotId).stream()
                .map(o -> new TrassirSnapshotObjectView(
                        o.getId(), o.getSnapshotId(), o.getObjectKey(),
                        o.getObjectPayload(), null))
                .toList();
    }

    public PageResponse<TrassirSystemEventView> listSystemEvents(UUID serverId, String eventType,
                                                                 String severity,
                                                                 Instant from, Instant to,
                                                                 Pageable pageable) {
        Page<TrassirSystemEventView> page = systemEventRepository
                .search(serverId, eventType, severity, from, to, pageable)
                .map(e -> new TrassirSystemEventView(
                        e.getId(),
                        e.getServerId() != null ? e.getServerId().toString() : null,
                        e.getEventId(), e.getEventType(), e.getEventVersion(),
                        e.getOccurredAt(), e.getObservedAt(), e.getSeverity(),
                        e.getSource(), e.getData(), e.getCreatedAt()));
        return PageResponse.of(page);
    }

    public PageResponse<TrassirEventView> listEvents(UUID serverId, String eventType, String status,
                                                     Instant from, Instant to, Pageable pageable) {
        Page<TrassirEventView> page = eventRepository
                .search(serverId, eventType, status, from, to, pageable)
                .map(e -> new TrassirEventView(
                        e.getId(),
                        e.getServerId() != null ? e.getServerId().toString() : null,
                        e.getEventId(), e.getEventIndex(), e.getEventType(), e.getEventVersion(),
                        e.getOccurredAt(), e.getObservedAt(), e.getStatus(),
                        e.getRetryCount(), e.getLastError(), e.getNextRetryAt(), e.getCreatedAt()));
        return PageResponse.of(page);
    }

    private TrassirServerView toServerView(TrassirServerState s) {
        return new TrassirServerView(
                s.getServerId(), s.getLastHeartbeatAt(), s.getLastSnapshotAt(), s.getUpdatedAt());
    }
}