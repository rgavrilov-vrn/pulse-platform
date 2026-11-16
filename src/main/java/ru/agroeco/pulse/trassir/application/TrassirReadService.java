package ru.agroeco.pulse.trassir.application;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.trassir.adapter.persistence.*;
import ru.agroeco.pulse.trassir.application.dto.*;
import ru.agroeco.pulse.trassir.domain.*;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrassirReadService {

    private static final char LIKE_ESCAPE = '\\';

    private final TrassirServerStateRepository serverStateRepository;
    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final TrassirSystemEventRepository systemEventRepository;
    private final TrassirEventRepository eventRepository;

    // ============================================================ servers

    public List<TrassirServerView> listServers() {
        return serverStateRepository.findAll().stream().map(this::toServerView).toList();
    }

    public TrassirServerView getServer(UUID serverId) {
        TrassirServerState state = serverStateRepository.findById(serverId)
                .orElseThrow(() -> new NotFoundException("TRASSIR server state not found: " + serverId));
        return toServerView(state);
    }

    // ============================================================ snapshots

    public PageResponse<TrassirSnapshotView> listSnapshots(UUID serverId, String status,
                                                           Instant from, Instant to, Pageable pageable) {
        Page<TrassirSnapshotView> page = snapshotRepository
                .findAll(snapshotSpec(serverId, status, from, to), pageable)
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

    // ============================================================ system-events

    public PageResponse<TrassirSystemEventView> listSystemEvents(UUID serverId, String eventType,
                                                                 String severity, String objectId,
                                                                 String q, Instant from, Instant to,
                                                                 Pageable pageable) {
        String pattern = q != null && !q.isBlank() ? QuerySupport.containsPattern(q) : null;
        return PageResponse.of(systemEventRepository
                .findAll(systemEventSpec(serverId, eventType, severity, objectId, pattern, from, to), pageable)
                .map(this::toSystemEventView));
    }

    public TrassirSystemEventView getSystemEvent(UUID id) {
        return systemEventRepository.findById(id).map(this::toSystemEventView)
                .orElseThrow(() -> new NotFoundException("System event not found: " + id));
    }

    public List<String> distinctSystemEventTypes(UUID serverId) {
        return systemEventRepository.distinctEventTypes(serverId);
    }

    public List<String> distinctSystemEventSeverities(UUID serverId) {
        return systemEventRepository.distinctSeverities(serverId);
    }

    public TrassirSystemEventStatsView systemEventStats(UUID serverId, Instant from, Instant to) {
        // Считаем total через Specification, чтобы не было дублирования с пагинацией
        long total = systemEventRepository.count(
                systemEventSpec(serverId, null, null, null, null, from, to));

        Instant fromEff = from != null ? from : Instant.EPOCH;
        Instant toEff   = to   != null ? to   : Instant.now().plusSeconds(365L * 24 * 3600);

        return new TrassirSystemEventStatsView(
                total,
                toMap(systemEventRepository.countBySeverity(serverId, fromEff, toEff)),
                toMap(systemEventRepository.countByEventType(serverId, fromEff, toEff)),
                toMap(systemEventRepository.countByObjectId(serverId, fromEff, toEff)));
    }

    // ============================================================ events

    public PageResponse<TrassirEventView> listEvents(UUID serverId, String eventType, String status,
                                                     Instant from, Instant to, Pageable pageable) {
        return PageResponse.of(eventRepository
                .findAll(eventSpec(serverId, eventType, status, from, to), pageable)
                .map(e -> new TrassirEventView(
                        e.getId(),
                        e.getServerId() != null ? e.getServerId().toString() : null,
                        e.getEventId(), e.getEventIndex(), e.getEventType(), e.getEventVersion(),
                        e.getOccurredAt(), e.getObservedAt(), e.getStatus(),
                        e.getRetryCount(), e.getLastError(), e.getNextRetryAt(), e.getCreatedAt())));
    }

    public Map<String, Long> eventStatusStats(UUID serverId) {
        return toMap(eventRepository.countByStatus(serverId));
    }

    // ============================================================ specifications

    private static Specification<TrassirSystemEvent> systemEventSpec(
            UUID serverId, String eventType, String severity, String objectId,
            String pattern, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (serverId  != null) p.add(cb.equal(root.get("serverId"), serverId));
            if (eventType != null) p.add(cb.equal(root.get("eventType"), eventType));
            if (severity  != null) p.add(cb.equal(root.get("severity"), severity));
            if (objectId  != null) p.add(cb.equal(root.get("objectId"), objectId));
            if (pattern   != null) {
                p.add(cb.or(
                        cb.like(cb.lower(root.get("objectName")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("eventType")),  pattern, LIKE_ESCAPE)));
            }
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("observedAt"), from));
            if (to   != null) p.add(cb.lessThanOrEqualTo(root.get("observedAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Specification<TrassirSnapshot> snapshotSpec(
            UUID serverId, String status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (serverId != null) p.add(cb.equal(root.get("serverId"), serverId));
            if (status   != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), from));
            if (to   != null) p.add(cb.lessThanOrEqualTo(root.get("receivedAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Specification<TrassirEvent> eventSpec(
            UUID serverId, String eventType, String status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (serverId  != null) p.add(cb.equal(root.get("serverId"), serverId));
            if (eventType != null) p.add(cb.equal(root.get("eventType"), eventType));
            if (status    != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to   != null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    // ============================================================ mappers

    private TrassirSystemEventView toSystemEventView(TrassirSystemEvent e) {
        return new TrassirSystemEventView(
                e.getId(),
                e.getServerId() != null ? e.getServerId().toString() : null,
                e.getEventId(), e.getEventType(), e.getEventVersion(),
                e.getOccurredAt(), e.getObservedAt(), e.getSeverity(),
                e.getObjectId(), e.getObjectName(),
                e.getP1(), e.getP2(), e.getP3(),
                e.getData(), e.getSource(), e.getCreatedAt());
    }

    private TrassirServerView toServerView(TrassirServerState s) {
        return new TrassirServerView(
                s.getServerId(), s.getLastHeartbeatAt(), s.getLastSnapshotAt(), s.getUpdatedAt());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String k = r[0] != null ? r[0].toString() : "null";
            m.put(k, ((Number) r[1]).longValue());
        }
        return m;
    }
}