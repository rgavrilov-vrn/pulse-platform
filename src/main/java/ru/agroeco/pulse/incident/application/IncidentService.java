package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.incident.adapter.http.dto.*;
import ru.agroeco.pulse.incident.adapter.persistence.*;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.*;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private static final String DEFAULT_SOURCE = "manual";

    private final IncidentRepository incidents;
    private final SourceRepository sources;
    private final CommentRepository comments;
    private final StatusHistoryRepository statusHistory;
    private final StatusTransitionRepository transitions;
    private final EventDispatcher events;

    // ------------------------------------------------------------------ CREATE
    @Transactional
    public IncidentDetails create(CreateIncidentRequest req, UUID actorId) {
        String sourceCode = (req.source() == null || req.source().isBlank())
                ? DEFAULT_SOURCE : req.source().trim();

        SourceEntity source = sources.findById(sourceCode)
                .filter(SourceEntity::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "Source '" + sourceCode + "' does not exist or is inactive"));

        Instant now = Instant.now();
        IncidentEntity incident = IncidentEntity.builder()
                .source(source.getCode())
                .creationMode(req.creationMode())
                .status(IncidentStatus.DRAFT)
                .severity(req.severity() != null ? req.severity() : IncidentSeverity.MEDIUM)
                .eventTime(req.eventTime())
                .receivedAt(now)
                .createdBy(actorId)
                .updatedBy(actorId)
                .comment(req.comment())
                .metadata(req.metadata() != null ? req.metadata() : Map.of())
                .build();

        incidents.save(incident);
        log.info("Incident created: id={}, number={}, source={}", incident.getId(), incident.getNumber(), sourceCode);

        // Фрагмент метода create — без изменений по логике, только новые nullable-поля
        events.dispatch(IncidentCreated.builder()
                .incidentId(incident.getId())
                .number(incident.getNumber())
                .source(source.getCode())
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .eventTime(incident.getEventTime())
                .actorId(actorId)
                .occurredAt(now)
                // sourceEventId, sourceEventType, videoServerId, cameraId,
                // channelId, objectId, title — остаются null (ручной путь)
                .build());

        return toDetails(incident);
    }

    // -------------------------------------------------------------------- LIST
    @Transactional(readOnly = true)
    public PageResponse<IncidentSummary> list(Specification<IncidentEntity> spec, Pageable pageable) {
        Page<IncidentEntity> page = incidents.findAll(spec, pageable);
        return PageResponse.of(page, this::toSummary);
    }

    // ----------------------------------------------------------- CHANGE STATUS
    @Transactional
    public IncidentDetails changeStatus(UUID id, ChangeStatusRequest req, UUID actorId) {
        IncidentEntity incident = incidents.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));

        IncidentStatus from = incident.getStatus();
        IncidentStatus to = req.toStatus();

        if (from == to) {
            throw new BusinessRuleException("Incident already has status " + to);
        }
        if (from.isTerminal()) {
            throw new BusinessRuleException("Status " + from + " is terminal; no transitions allowed");
        }
        var tid = new StatusTransitionId(from, to);
        if (!transitions.existsById(tid)) {
            throw new BusinessRuleException(
                    "Transition " + from + " → " + to + " is not allowed");
        }

        Instant now = Instant.now();
        incident.setStatus(to);
        incident.setUpdatedBy(actorId);

        // side-effects по типу статуса — по контракту DB
        switch (to) {
            case CONFIRMED -> { incident.setConfirmedAt(now); incident.setConfirmedBy(actorId); }
            case CLOSED    -> {
                incident.setClosedAt(now);
                incident.setClosedBy(actorId);
                if (req.comment() != null && !req.comment().isBlank()) {
                    incident.setResolutionComment(req.comment());
                }
            }
            case CANCELLED -> {
                incident.setCancelledAt(now);
                incident.setCancelledBy(actorId);
                incident.setCancellationReason(req.comment());
            }
            default -> { /* остальные переходы без side-effects */ }
        }

        incidents.save(incident);

        statusHistory.save(StatusHistoryEntity.builder()
                .incidentId(id)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(actorId)
                .comment(req.comment())
                .build());

        events.dispatch(IncidentStatusChanged.builder()
                .incidentId(id)
                .fromStatus(from)
                .toStatus(to)
                .comment(req.comment())
                .actorId(actorId)
                .occurredAt(now)
                .build());

        log.info("Incident {} status changed: {} → {} by {}", id, from, to, actorId);
        return toDetails(incident);
    }

    // ------------------------------------------------------------ ADD COMMENT
    @Transactional
    public CommentView addComment(UUID incidentId, AddCommentRequest req, UUID actorId) {
        if (!incidents.existsById(incidentId)) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }

        CommentEntity c = CommentEntity.builder()
                .incidentId(incidentId)
                .authorId(actorId)
                .commentType(req.commentType())
                .body(req.body().trim())
                .deleted(false)
                .build();
        comments.save(c);

        events.dispatch(IncidentCommentAdded.builder()
                .incidentId(incidentId)
                .commentId(c.getId())
                .authorId(actorId)
                .commentType(c.getCommentType())
                .occurredAt(c.getCreatedAt() != null ? c.getCreatedAt() : Instant.now())
                .build());

        return new CommentView(c.getId(), c.getIncidentId(), c.getAuthorId(),
                c.getCommentType(), c.getBody(), c.getCreatedAt());
    }

    // ---------------------------------------------------------------- MAPPERS
    private IncidentSummary toSummary(IncidentEntity i) {
        return new IncidentSummary(i.getId(), i.getNumber(), i.getSource(),
                i.getCreationMode(), i.getStatus(), i.getSeverity(),
                i.getEventTime(), i.getCreatedAt(), i.getUpdatedAt(),
                i.getAssignedTo(), i.getComment());
    }

    private IncidentDetails toDetails(IncidentEntity i) {
        return new IncidentDetails(i.getId(), i.getNumber(), i.getVersion(),
                i.getSource(), i.getCreationMode(), i.getStatus(), i.getSeverity(),
                i.getEventTime(), i.getReceivedAt(), i.getCreatedAt(), i.getUpdatedAt(),
                i.getCreatedBy(), i.getUpdatedBy(), i.getOperatorId(), i.getAssignedTo(),
                i.getDueAt(), i.getConfirmedAt(), i.getConfirmedBy(),
                i.getClosedAt(), i.getClosedBy(), i.getResolutionComment(),
                i.getCancelledAt(), i.getCancelledBy(), i.getCancellationReason(),
                i.getComment(), i.getMetadata());
    }
}