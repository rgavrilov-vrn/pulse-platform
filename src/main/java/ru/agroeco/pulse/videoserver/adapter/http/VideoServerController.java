package ru.agroeco.pulse.videoserver.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.*;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.application.CreateVideoServerUseCase;
import ru.agroeco.pulse.videoserver.application.UpdateVideoServerUseCase;
import ru.agroeco.pulse.videoserver.domain.VideoServer;
import ru.agroeco.pulse.videoserver.domain.VideoServerStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videoserver/servers")
@RequiredArgsConstructor
@Tag(name = "Video Servers")
public class VideoServerController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "name", "source", "status", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CreateVideoServerUseCase createUseCase;
    private final UpdateVideoServerUseCase updateUseCase;
    private final VideoServerRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('video:write')")
    @Operation(summary = "Create video server")
    public VideoServerResponse create(@Valid @RequestBody CreateVideoServerRequest req) {
        return createUseCase.handle(req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "List video servers")
    public PageResponse<VideoServerResponse> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID objectId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC) Pageable pageable) {
        Pageable safe = sanitize(pageable);
        VideoServerStatus statusFilter = parseStatusFilter(status);
        Specification<VideoServer> spec = buildSpec(source, objectId, zoneId, statusFilter, isActive, search);
        return PageResponse.of(repository.findAll(spec, safe), this::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "Get video server by id")
    public VideoServerResponse get(@PathVariable UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Video server not found: " + id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('video:write')")
    @Operation(summary = "Update video server")
    public VideoServerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVideoServerRequest req) {
        return updateUseCase.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('video:delete')")
    @Operation(summary = "Soft-delete video server")
    public void delete(@PathVariable UUID id) {
        updateUseCase.delete(id);
    }

    private Pageable sanitize(Pageable pageable) {
        List<Sort.Order> valid = pageable.getSort().stream()
                .filter(o -> ALLOWED_SORT.contains(o.getProperty())).toList();
        Sort sort = valid.isEmpty()
                ? Sort.by(Sort.Direction.ASC, DEFAULT_SORT) : Sort.by(valid);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Specification<VideoServer> buildSpec(String source, UUID objectId, UUID zoneId,
                                                 VideoServerStatus status, Boolean isActive, String search) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (source != null && !source.isBlank()) p.add(cb.equal(root.get("source"), source));
            if (objectId != null) p.add(cb.equal(root.get("objectId"), objectId));
            if (zoneId != null) p.add(cb.equal(root.get("zoneId"), zoneId));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (isActive != null) p.add(cb.equal(root.get("isActive"), isActive));
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")),
                        QuerySupport.containsPattern(search), '\\'));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private VideoServerStatus parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return VideoServerStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    "INVALID_STATUS: '" + raw + "' (allowed: ONLINE, OFFLINE, UNKNOWN)");
        }
    }

    private VideoServerResponse toResponse(VideoServer s) {
        return new VideoServerResponse(
                s.getId(), s.getObjectId(), s.getZoneId(),
                s.getSource(), s.getExternalId(), s.getName(),
                s.getHost(), s.getBaseUrl(), s.getDescription(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}