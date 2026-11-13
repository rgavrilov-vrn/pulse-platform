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
import ru.agroeco.pulse.trassir.application.dto.TrassirEventView;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trassir/events")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Read")
public class TrassirReadEventController {

    private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "observedAt", "occurredAt", "id");
    private static final String DEFAULT_SORT = "createdAt";

    private final TrassirReadService readService;

    @GetMapping
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List raw trassir.events")
    public PageResponse<TrassirEventView> list(
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = DEFAULT_SORT, direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return readService.listEvents(serverId, eventType, status, from, to, safe);
    }
}