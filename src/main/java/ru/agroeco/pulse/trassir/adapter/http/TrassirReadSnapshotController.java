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
import ru.agroeco.pulse.trassir.application.dto.TrassirSnapshotObjectView;
import ru.agroeco.pulse.trassir.application.dto.TrassirSnapshotView;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trassir/snapshots")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Read")
public class TrassirReadSnapshotController {

    private static final Set<String> ALLOWED_SORT = Set.of("receivedAt", "completedAt", "id");
    private static final String DEFAULT_SORT = "receivedAt";

    private final TrassirReadService readService;

    @GetMapping
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List snapshots")
    public PageResponse<TrassirSnapshotView> list(
            @RequestParam(required = false) UUID serverId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = DEFAULT_SORT, direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.DESC);
        return readService.listSnapshots(serverId, status, from, to, safe);
    }

    @GetMapping("/{snapshotId}/objects")
    @PreAuthorize("hasAuthority('trassir:admin')")
    @Operation(summary = "List objects of a snapshot")
    public List<TrassirSnapshotObjectView> objects(@PathVariable UUID snapshotId) {
        return readService.listSnapshotObjects(snapshotId);
    }
}