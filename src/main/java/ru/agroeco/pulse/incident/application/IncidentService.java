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
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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

    // ↓ новые зависимости для чек-листов
    private final IncidentChecklistItemRepository checklistLinks;
    private final ChecklistItemRepository checklistItems;
    private final ViolationCategoryRepository categories;
    private final CategoryGroupRepository groups;

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

        incidents.saveAndFlush(incident);
        log.info("Incident created: id={}, number={}, source={}",
                incident.getId(), incident.getNumber(), sourceCode);

        events.dispatch(IncidentCreated.builder()
                .incidentId(incident.getId())
                .number(incident.getNumber())
                .source(source.getCode())
                .status(incident.getStatus())
                .severity(incident.getSeverity())
                .eventTime(incident.getEventTime())
                .actorId(actorId)
                .occurredAt(now)
                .build());

        return toDetails(incident);
    }

    // -------------------------------------------------------------------- LIST
    @Transactional(readOnly = true)
    public PageResponse<IncidentSummary> list(Specification<IncidentEntity> spec, Pageable pageable) {
        Page<IncidentEntity> page = incidents.findAll(spec, pageable);
        return PageResponse.of(page, this::toSummary);
    }

    // ---------------------------------------------------------------- GET BY ID
    @Transactional(readOnly = true)
    public IncidentDetails getById(UUID id) {
        IncidentEntity incident = incidents.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));
        return toDetails(incident);
    }

    // ------------------------------------------------------------- LIST COMMENTS
    @Transactional(readOnly = true)
    public PageResponse<CommentView> listComments(UUID incidentId, Pageable pageable) {
        if (!incidents.existsById(incidentId)) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        Page<CommentEntity> page = comments.findByIncidentIdAndDeletedFalse(incidentId, pageable);
        return PageResponse.of(page, this::toCommentView);
    }

    // -------------------------------------------------------------- LIST HISTORY
    @Transactional(readOnly = true)
    public PageResponse<StatusHistoryView> listHistory(UUID incidentId, Pageable pageable) {
        if (!incidents.existsById(incidentId)) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        Page<StatusHistoryEntity> page = statusHistory.findByIncidentId(incidentId, pageable);
        return PageResponse.of(page, this::toHistoryView);
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

        incidents.saveAndFlush(incident);

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
        comments.saveAndFlush(c);

        events.dispatch(IncidentCommentAdded.builder()
                .incidentId(incidentId)
                .commentId(c.getId())
                .authorId(actorId)
                .commentType(c.getCommentType())
                .occurredAt(c.getCreatedAt() != null ? c.getCreatedAt() : Instant.now())
                .build());

        return toCommentView(c);
    }

    // ---------------------------------------------------------------- MAPPERS
    private IncidentSummary toSummary(IncidentEntity i) {
        return new IncidentSummary(
                i.getId(),
                i.getNumber(),
                i.getSource(),
                i.getCreationMode(),
                i.getStatus(),
                i.getSeverity(),
                i.getKind(),                    // ← NEW, после severity
                i.getEventTime(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                i.getAssignedTo(),
                i.getComment()
        );
    }

    private IncidentDetails toDetails(IncidentEntity i) {
        List<IncidentChecklistItemView> checkpoints =
                checklistLinks.findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(i.getId())
                        .stream().map(this::toCheckpointView).toList();

        return new IncidentDetails(
                i.getId(),
                i.getNumber(),
                i.getVersion(),
                i.getSource(),
                i.getCreationMode(),
                i.getStatus(),
                i.getSeverity(),
                i.getKind(),                    // ← порядок как в record
                checkpoints,                    // ← порядок как в record
                i.getEventTime(),
                i.getReceivedAt(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                i.getCreatedBy(),
                i.getUpdatedBy(),
                i.getOperatorId(),
                i.getAssignedTo(),
                i.getDueAt(),
                i.getConfirmedAt(),
                i.getConfirmedBy(),
                i.getClosedAt(),
                i.getClosedBy(),
                i.getResolutionComment(),
                i.getCancelledAt(),
                i.getCancelledBy(),
                i.getCancellationReason(),
                i.getComment(),
                i.getMetadata()
        );
    }

    private CommentView toCommentView(CommentEntity c) {
        return new CommentView(c.getId(), c.getIncidentId(), c.getAuthorId(),
                c.getCommentType(), c.getBody(), c.getCreatedAt());
    }

    private StatusHistoryView toHistoryView(StatusHistoryEntity h) {
        return new StatusHistoryView(h.getId(), h.getIncidentId(),
                h.getFromStatus(), h.getToStatus(), h.getChangedBy(),
                h.getChangedAt(), h.getComment(), h.getRequestId());
    }

    // -------------------------------------------------------------- UPDATE
    @Transactional
    public IncidentDetails update(UUID id, UpdateIncidentRequest req, UUID actorId) {
        IncidentEntity incident = incidents.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));

        if (incident.getStatus().isTerminal()) {
            throw new BusinessRuleException(
                    "Incident " + incident.getStatus() + " is terminal; update not allowed");
        }

        Map<String, Object> oldData = new HashMap<>();
        if (incident.getKind() != null) oldData.put("kind", incident.getKind().name());
        oldData.put("eventTime", incident.getEventTime().toString());
        if (incident.getComment() != null) oldData.put("comment", incident.getComment());

        if (req.kind() != null)      incident.setKind(req.kind());
        if (req.eventTime() != null) incident.setEventTime(req.eventTime());
        if (req.comment() != null)   incident.setComment(req.comment().isBlank() ? null : req.comment());

        if (req.checkpoint() != null && !req.checkpoint().isBlank()) {
            addCheckpointLink(incident, req.checkpoint(), actorId);
        }

        // Валидация согласованности kind ↔ существующие линки
        if (!checklistLinks.findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(id).isEmpty()
                && incident.getKind() == null) {
            throw new BusinessRuleException(
                    "Incident has checkpoints but kind is null");
        }

        incident.setUpdatedBy(actorId);
        incidents.saveAndFlush(incident);

        addComment(incident.getId(), new AddCommentRequest(CommentType.OPERATOR, req.comment()), actorId);

        events.dispatch(IncidentUpdated.builder()
                .incidentId(id)
                .kind(incident.getKind())
                .eventTime(incident.getEventTime())
                .comment(incident.getComment())
                .oldData(oldData)
                .actorId(actorId)
                .occurredAt(Instant.now())
                .build());

        return toDetails(incident);
    }

    // ------------------------------------------- CHECKPOINT (add)
    @Transactional
    public IncidentChecklistItemView addCheckpoint(
            UUID incidentId, String checklistItemCode, String comment, UUID actorId) {
        IncidentEntity incident = incidents.findById(incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + incidentId));

        if (incident.getStatus().isTerminal()) {
            throw new BusinessRuleException(
                    "Incident " + incident.getStatus() + " is terminal; cannot add checkpoints");
        }

        IncidentChecklistItemEntity link =
                addCheckpointLink(incident, checklistItemCode, comment, actorId);
        incidents.saveAndFlush(incident);   // сохранить kind, если авто-вывелся

        events.dispatch(IncidentCheckpointAdded.builder()
                .incidentId(incidentId)
                .linkId(link.getId())
                .checklistItemId(link.getChecklistItemId())
                .checklistItemCode(checklistItemCode)
                .kind(incident.getKind())
                .actorId(actorId)
                .occurredAt(Instant.now())
                .build());

        return toCheckpointView(link);
    }

    // ------------------------------------------- CHECKPOINT (list)
    @Transactional(readOnly = true)
    public List<IncidentChecklistItemView> listCheckpoints(UUID incidentId) {
        if (!incidents.existsById(incidentId)) {
            throw new NotFoundException("Incident not found: " + incidentId);
        }
        return checklistLinks
                .findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(incidentId)
                .stream().map(this::toCheckpointView).toList();
    }

    // ------------------------------------------- CHECKPOINT (remove, soft)
    @Transactional
    public void removeCheckpoint(UUID incidentId, UUID linkId, UUID actorId) {
        IncidentChecklistItemEntity link = checklistLinks.findById(linkId)
                .filter(l -> l.getIncidentId().equals(incidentId))
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new NotFoundException("Checkpoint link not found: " + linkId));

        link.setDeleted(true);
        link.setDeletedAt(Instant.now());
        link.setDeletedBy(actorId);
        checklistLinks.saveAndFlush(link);

        events.dispatch(IncidentCheckpointRemoved.builder()
                .incidentId(incidentId)
                .linkId(linkId)
                .checklistItemId(link.getChecklistItemId())
                .actorId(actorId)
                .occurredAt(Instant.now())
                .build());
    }

    // ------------------------------------------- helper
    private IncidentChecklistItemEntity addCheckpointLink(
            IncidentEntity incident, String code, UUID actorId) {
        return addCheckpointLink(incident, code, null, actorId);
    }

    private IncidentChecklistItemEntity addCheckpointLink(
            IncidentEntity incident, String code, String comment, UUID actorId) {

        ChecklistItemEntity item = checklistItems.findByCode(code)
                .orElseThrow(() -> new BusinessRuleException("Unknown checkpoint: " + code));

        // Идемпотентность: если уже привязан — вернуть существующий
        var existing = checklistLinks
                .findByIncidentIdAndChecklistItemIdAndDeletedFalse(incident.getId(), item.getId());
        if (existing.isPresent()) return existing.get();

        ViolationCategoryEntity category = categories.findById(item.getCategoryId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Category missing for checkpoint " + code));
        CategoryGroupEntity group = groups.findById(category.getGroupId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Group missing for category " + category.getCode()));

        IncidentKind derivedKind = IncidentKind.fromGroupCode(group.getCode());

/*
        if (incident.getKind() != null && incident.getKind() != derivedKind) {
            throw new BusinessRuleException(
                    "Checkpoint '" + code + "' belongs to kind " + derivedKind
                            + ", but incident kind is " + incident.getKind());
        }
 */
        if (incident.getKind() == null) {
            incident.setKind(derivedKind);
        }

        IncidentChecklistItemEntity link = IncidentChecklistItemEntity.builder()
                .incidentId(incident.getId())
                .checklistItemId(item.getId())
                .itemNameSnapshot(item.getName())
                .categoryNameSnapshot(category.getName())
                .severity(item.getDefaultSeverity())
                .comment(comment)
                .createdBy(actorId)
                .deleted(false)
                .build();

        return checklistLinks.save(link);
    }

    // ------------------------------------------- mappers
    private IncidentChecklistItemView toCheckpointView(IncidentChecklistItemEntity l) {
        return new IncidentChecklistItemView(
                l.getId(), l.getIncidentId(), l.getChecklistItemId(),
                l.getItemNameSnapshot(), l.getCategoryNameSnapshot(),
                l.getSeverity(), l.getComment(), l.getCreatedBy(), l.getCreatedAt());
    }
}