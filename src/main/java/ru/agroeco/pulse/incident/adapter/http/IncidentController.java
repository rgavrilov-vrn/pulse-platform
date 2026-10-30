package ru.agroeco.pulse.incident.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.incident.adapter.http.dto.*;
import ru.agroeco.pulse.incident.application.IncidentService;
import ru.agroeco.pulse.incident.domain.IncidentEntity;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Управление инцидентами")
public class IncidentController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "number", "eventTime", "createdAt", "updatedAt", "status", "severity", "source", "id");
    private static final String DEFAULT_SORT = "eventTime";
    private static final String HISTORY_DEFAULT_SORT = "changedAt";

    private final IncidentService incidentService;

    // ------------------------------------------------------------------ CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create incident")
    public IncidentDetails create(
            @Valid @RequestBody CreateIncidentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.create(req, principal.id());
    }

    // -------------------------------------------------------------------- LIST
    @GetMapping
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incidents with filters and pagination",
            description = "Фильтры: status, severity, source (exact), assignedTo, "
                    + "eventFrom/eventTo (ISO-8601, from ≤ to). "
                    + "Sort: number, eventTime, createdAt, updatedAt, status, severity, source, id. "
                    + "Size ограничен 100.")
    public PageResponse<IncidentSummary> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant eventFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant eventTo,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        validateRange(eventFrom, eventTo);
        Pageable safe = QuerySupport.sanitize(
                pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return incidentService.list(
                buildSpec(status, severity, source, assignedTo, eventFrom, eventTo), safe);
    }

    // --------------------------------------------------------------- GET BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "Get incident by id")
    public IncidentDetails getById(@PathVariable UUID id) {
        return incidentService.getById(id);
    }

    // ----------------------------------------------------------- LIST COMMENTS
    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incident comments (paginated, not deleted)",
            description = "Sort: createdAt, id. Default: createdAt, DESC.")
    public PageResponse<CommentView> listComments(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        // Явно ограничиваем сортировку по существующим полям.
        Pageable safe = QuerySupport.sanitize(
                pageable, Set.of("createdAt", "id"), "createdAt", Sort.Direction.DESC);
        return incidentService.listComments(id, safe);
    }

    // -------------------------------------------------------------- LIST HISTORY
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incident status change history (paginated)",
            description = "Sort: changedAt, id. Default: changedAt, DESC.")
    public PageResponse<StatusHistoryView> listHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = HISTORY_DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(
                pageable, Set.of("changedAt", "id"), HISTORY_DEFAULT_SORT, Sort.Direction.DESC);
        return incidentService.listHistory(id, safe);
    }

    // ----------------------------------------------------------- CHANGE STATUS
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('incident:write', 'incident:review')")
    @Operation(summary = "Change incident status (validated against status_transitions)")
    public IncidentDetails changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.changeStatus(id, req, principal.id());
    }

    // ------------------------------------------------------------ ADD COMMENT
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add comment to incident")
    public CommentView addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.addComment(id, req, principal.id());
    }

    // -------------------------------------------------------------- UPDATE
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('incident:write')")
    @Operation(summary = "Partial update incident",
            description = "Обновляет kind, eventTime, comment. Если передан checkpoint "
                    + "(incident.checklist_items.code) — добавляет связь с пунктом чек-листа "
                    + "идемпотентно, kind выводится из группы чек-листа при необходимости.")
    public IncidentDetails update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIncidentRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.update(id, req, principal.id());
    }

    // ------------------------------------------- LIST CHECKPOINTS
    @GetMapping("/{id}/checkpoints")
    @PreAuthorize("hasAuthority('incident:read')")
    @Operation(summary = "List incident checkpoints (active links)")
    public List<IncidentChecklistItemView> listCheckpoints(@PathVariable UUID id) {
        return incidentService.listCheckpoints(id);
    }

    // ------------------------------------------- ADD CHECKPOINT
    @PostMapping("/{id}/checkpoints")
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Attach checklist item to incident (idempotent)")
    public IncidentChecklistItemView addCheckpoint(
            @PathVariable UUID id,
            @Valid @RequestBody AddIncidentCheckpointRequest req,
            @AuthenticationPrincipal AppPrincipal principal) {
        return incidentService.addCheckpoint(id, req.checkpoint(), req.comment(), principal.id());
    }

    // ------------------------------------------- REMOVE CHECKPOINT (soft)
    @DeleteMapping("/{id}/checkpoints/{linkId}")
    @PreAuthorize("hasAuthority('incident:write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Detach checklist item (soft delete)")
    public void removeCheckpoint(
            @PathVariable UUID id,
            @PathVariable UUID linkId,
            @AuthenticationPrincipal AppPrincipal principal) {
        incidentService.removeCheckpoint(id, linkId, principal.id());
    }

    // --------------------------------------------------------------- helpers

    private Specification<IncidentEntity> buildSpec(
            IncidentStatus status, IncidentSeverity severity, String source,
            UUID assignedTo, Instant eventFrom, Instant eventTo) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null)     p.add(cb.equal(root.get("status"), status));
            if (severity != null)   p.add(cb.equal(root.get("severity"), severity));
            if (source != null && !source.isBlank()) p.add(cb.equal(root.get("source"), source));
            if (assignedTo != null) p.add(cb.equal(root.get("assignedTo"), assignedTo));
            if (eventFrom != null)  p.add(cb.greaterThanOrEqualTo(root.get("eventTime"), eventFrom));
            if (eventTo != null)    p.add(cb.lessThanOrEqualTo(root.get("eventTime"), eventTo));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE: 'eventFrom' must be <= 'eventTo' (from=" + from + ", to=" + to + ")");
        }
    }
}