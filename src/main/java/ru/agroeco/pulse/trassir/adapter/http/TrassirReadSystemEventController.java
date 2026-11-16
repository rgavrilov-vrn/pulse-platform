package ru.agroeco.pulse.trassir.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.trassir.application.TrassirReadService;
import ru.agroeco.pulse.trassir.application.dto.TrassirSystemEventStatsView;
import ru.agroeco.pulse.trassir.application.dto.TrassirSystemEventView;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trassir/system-events")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · System Events", description = "События из TRASSIR SDK")
public class TrassirReadSystemEventController {

    private static final Set<String> ALLOWED_SORT = Set.of("observedAt", "occurredAt", "createdAt", "id");
    private static final String DEFAULT_SORT = "observedAt";

    private final TrassirReadService readService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('trassir:admin','incident:read','video_event:read')")
    @Operation(summary = "List system events (с full-text поиском по objectName/eventType)")
    public PageResponse<TrassirSystemEventView> list(
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = DEFAULT_SORT, direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return readService.listSystemEvents(serverId, eventType, severity, objectId, q, from, to, safe);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('trassir:admin','incident:read','video_event:read')")
    @Operation(summary = "Get one system event")
    public TrassirSystemEventView get(@PathVariable UUID id) {
        return readService.getSystemEvent(id);
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('trassir:admin','incident:read','video_event:read')")
    @Operation(summary = "Distinct event types (для dropdown)")
    public List<String> types(@RequestParam(required = false) UUID serverId) {
        return readService.distinctSystemEventTypes(serverId);
    }

    @GetMapping("/severities")
    @PreAuthorize("hasAnyAuthority('trassir:admin','incident:read','video_event:read')")
    @Operation(summary = "Distinct severities (для dropdown)")
    public List<String> severities(@RequestParam(required = false) UUID serverId) {
        return readService.distinctSystemEventSeverities(serverId);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('trassir:admin','incident:read','video_event:read')")
    @Operation(summary = "Aggregate stats: total, bySeverity, byEventType, byObjectId")
    public TrassirSystemEventStatsView stats(
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return readService.systemEventStats(serverId, from, to);
    }
}