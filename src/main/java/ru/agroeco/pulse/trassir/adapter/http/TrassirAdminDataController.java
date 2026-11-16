package ru.agroeco.pulse.trassir.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.trassir.application.TrassirAdminAuditService;
import ru.agroeco.pulse.trassir.application.TrassirAdminOperationsService;
import ru.agroeco.pulse.trassir.application.TrassirDataPurgeService;
import ru.agroeco.pulse.trassir.application.TrassirReconcileService;
import ru.agroeco.pulse.trassir.application.dto.AdminAuditLogView;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/trassir/admin")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Admin", description = "Управляющие операции + аудит")
public class TrassirAdminDataController {

    private static final Set<String> AUDIT_ALLOWED_SORT = Set.of(
            "createdAt", "action", "actor", "status", "id");
    private static final String AUDIT_DEFAULT_SORT = "createdAt";

    private final TrassirDataPurgeService purgeService;
    private final TrassirReconcileService reconcileService;
    private final TrassirAdminOperationsService ops;
    private final TrassirAdminAuditService audit;

    // ============================================================ PURGE

    @DeleteMapping("/data")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "TRUNCATE all trassir.* tables (dangerous)")
    public Map<String, Object> purgeAll() {
        try {
            Map<String, Object> result = purgeService.purgeAll();
            audit.recordSuccess("PURGE_ALL", "trassir", result, null);
            return result;
        } catch (RuntimeException e) {
            audit.recordFailure("PURGE_ALL", "trassir", Map.of(), e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/system-events")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Purge system_events before given time")
    public Map<String, Object> purgeSystemEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before) {
        Map<String, Object> target = Map.of("before", before.toString());
        try {
            int n = ops.purgeSystemEvents(before);
            audit.recordSuccess("PURGE_SYSTEM_EVENTS", "trassir.system_events", target, n);
            return Map.of("deleted", n, "before", before);
        } catch (RuntimeException e) {
            audit.recordFailure("PURGE_SYSTEM_EVENTS", "trassir.system_events", target, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/events")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Purge finished events (PROCESSED/DEAD_LETTER) before given time")
    public Map<String, Object> purgeFinishedEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before) {
        Map<String, Object> target = Map.of("before", before.toString());
        try {
            int n = ops.purgeFinishedEvents(before);
            audit.recordSuccess("PURGE_EVENTS", "trassir.events", target, n);
            return Map.of("deleted", n, "before", before);
        } catch (RuntimeException e) {
            audit.recordFailure("PURGE_EVENTS", "trassir.events", target, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/ingest-raw/{batchId}")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Delete one ingest_raw batch (dangerous; incident response)")
    public Map<String, Object> deleteBatch(
            @PathVariable UUID batchId,
            @RequestParam(defaultValue = "false") boolean cascadeEvents) {
        Map<String, Object> target = Map.of(
                "batchId", batchId.toString(), "cascadeEvents", cascadeEvents);
        try {
            Map<String, Object> result = ops.deleteBatch(batchId, cascadeEvents);
            audit.recordSuccess("DELETE_BATCH", "trassir.ingest_raw", target, 1);
            return result;
        } catch (IllegalStateException | NotFoundException e) {
            audit.recordFailure("DELETE_BATCH", "trassir.ingest_raw", target, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (RuntimeException e) {
            audit.recordFailure("DELETE_BATCH", "trassir.ingest_raw", target, e.getMessage());
            throw e;
        }
    }

    // ============================================================ RETRY

    @PostMapping("/events/{id}/retry")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Retry one FAILED/DEAD_LETTER event")
    public Map<String, Object> retryEvent(@PathVariable UUID id) {
        try {
            int n = ops.retryEvent(id);
            if (n == 0) {
                audit.recordFailure("RETRY_EVENT", "trassir.events",
                        Map.of("eventId", id.toString()),
                        "Event not found or not in FAILED/DEAD_LETTER");
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Event not found or not in FAILED/DEAD_LETTER");
            }
            audit.recordSuccess("RETRY_EVENT", "trassir.events",
                    Map.of("eventId", id.toString()), n);
            return Map.of("eventId", id, "affected", n);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            audit.recordFailure("RETRY_EVENT", "trassir.events",
                    Map.of("eventId", id.toString()), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/events/retry")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Bulk retry FAILED/DEAD_LETTER events с фильтром")
    public Map<String, Object> retryEventsBulk(
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from) {
        Map<String, Object> target = new LinkedHashMap<>();
        if (serverId != null) target.put("serverId", serverId.toString());
        if (eventType != null) target.put("eventType", eventType);
        if (from != null) target.put("from", from.toString());

        try {
            int n = ops.retryEventsBulk(serverId, eventType, from);
            audit.recordSuccess("RETRY_EVENTS_BULK", "trassir.events", target, n);
            return Map.of("affected", n, "filter", target);
        } catch (RuntimeException e) {
            audit.recordFailure("RETRY_EVENTS_BULK", "trassir.events", target, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/snapshots/{id}/retry")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Retry snapshot assembly")
    public Map<String, Object> retrySnapshot(@PathVariable UUID id) {
        try {
            Map<String, Object> result = ops.retrySnapshot(id);
            audit.recordSuccess("RETRY_SNAPSHOT", "trassir.snapshots",
                    Map.of("snapshotId", id.toString()), 1);
            return result;
        } catch (RuntimeException e) {
            audit.recordFailure("RETRY_SNAPSHOT", "trassir.snapshots",
                    Map.of("snapshotId", id.toString()), e.getMessage());
            throw e;
        }
    }

    // ============================================================ RECONCILE

    @PostMapping("/snapshots/{snapshotId}/reconcile")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Re-dispatch snapshot для повторной реконсиляции")
    public Map<String, Object> reconcileSnapshot(@PathVariable UUID snapshotId) {
        try {
            Map<String, Object> result = reconcileService.reconcileSnapshot(snapshotId);
            audit.recordSuccess("RECONCILE_SNAPSHOT", "trassir.snapshots",
                    Map.of("snapshotId", snapshotId.toString()), 1);
            return result;
        } catch (RuntimeException e) {
            audit.recordFailure("RECONCILE_SNAPSHOT", "trassir.snapshots",
                    Map.of("snapshotId", snapshotId.toString()), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/servers/{serverId}/reconcile-latest")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Reconcile последнего completed snapshot сервера")
    public Map<String, Object> reconcileLatest(@PathVariable UUID serverId) {
        try {
            Map<String, Object> result = reconcileService.reconcileLatestCompletedSnapshot(serverId);
            audit.recordSuccess("RECONCILE_LATEST", "trassir.snapshots",
                    Map.of("serverId", serverId.toString()), 1);
            return result;
        } catch (RuntimeException e) {
            audit.recordFailure("RECONCILE_LATEST", "trassir.snapshots",
                    Map.of("serverId", serverId.toString()), e.getMessage());
            throw e;
        }
    }

    // ============================================================ AUDIT

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List admin audit log с фильтрами")
    public PageResponse<AdminAuditLogView> auditLog(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = AUDIT_DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, AUDIT_ALLOWED_SORT, AUDIT_DEFAULT_SORT, Sort.Direction.DESC);
        Page<AdminAuditLogView> page = audit.search(actor, action, status, from, to, safe)
                .map(e -> new AdminAuditLogView(
                        e.getId(), e.getActor(), e.getAction(), e.getTarget(),
                        e.getDetails(), e.getCorrelationId(), e.getRequestId(),
                        e.getStatus(), e.getErrorMessage(), e.getAffectedCount(), e.getCreatedAt()));
        return PageResponse.of(page);
    }

    @GetMapping("/audit/actions")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Distinct actions (для dropdown)")
    public List<String> auditActions() { return audit.distinctActions(); }

    @GetMapping("/audit/actors")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Distinct actors (для dropdown)")
    public List<String> auditActors() { return audit.distinctActors(); }
}