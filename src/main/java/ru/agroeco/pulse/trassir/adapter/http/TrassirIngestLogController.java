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
import ru.agroeco.pulse.trassir.application.TrassirIngestLogService;
import ru.agroeco.pulse.trassir.application.dto.TrassirIngestLogView;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trassir/admin/ingest-log")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Ingest Log", description = "Журнал HTTP-запросов на /trassir/ingest")
public class TrassirIngestLogController {

    private static final Set<String> ALLOWED_SORT = Set.of("receivedAt", "durationMs", "httpStatus", "id");
    private static final String DEFAULT_SORT = "receivedAt";

    private final TrassirIngestLogService service;

    @GetMapping
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List ingest log с фильтрами")
    public PageResponse<TrassirIngestLogView> list(
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String responseCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = DEFAULT_SORT, direction = Sort.Direction.DESC)
            Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return PageResponse.of(service.search(batchId, serverId, httpStatus, responseCode, from, to, safe));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Get ingest log (full detail)")
    public TrassirIngestLogView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-batch/{batchId}")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "All logs for a given batch_id")
    public List<TrassirIngestLogView> byBatch(@PathVariable UUID batchId) {
        return service.findByBatchId(batchId);
    }

    /**
     * Вложенный ответ: {"byHttpStatus": {"200": 10}, "byResponseCode": {"OK": 10}}
     * Фронт рендерит по группам.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Aggregate stats: byHttpStatus, byResponseCode")
    public Map<String, Map<String, Long>> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.stats(from, to);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "Purge ingest log before given time")
    public Map<String, Object> purge(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before) {
        int n = service.purgeBefore(before);
        return Map.of("deleted", n, "before", before);
    }
}