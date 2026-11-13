package ru.agroeco.pulse.videoserver.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.*;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.application.CameraService;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.CameraStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videoserver/cameras")
@RequiredArgsConstructor
@Tag(name = "Video Servers")
public class CameraController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "name", "externalId", "code", "status", "isActive", "lastSeenAt", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CameraRepository repository;
    private final CameraService service;

    @GetMapping
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "List cameras")
    public PageResponse<CameraResponse> list(
            @RequestParam(required = false) UUID videoServerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CameraStatus status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC) Pageable pageable) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        Specification<Camera> spec = buildSpec(videoServerId, q, status, isActive, includeDeleted);
        return PageResponse.of(repository.findAll(spec, safe), CameraResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "Get camera")
    public CameraResponse get(@PathVariable UUID id) {
        return repository.findById(id).map(CameraResponse::from)
                .orElseThrow(() -> new NotFoundException("Camera not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('video:write')")
    @Operation(summary = "Create camera")
    public CameraResponse create(@Valid @RequestBody CreateCameraRequest req) {
        return service.create(req);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('video:write')")
    @Operation(summary = "Update camera (name/code/description)")
    public CameraResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCameraRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('video:delete')")
    @Operation(summary = "Soft-delete camera (deactivated_by='manual' → reconcile не тронет)")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private Specification<Camera> buildSpec(UUID videoServerId, String q, CameraStatus status,
                                            Boolean isActive, boolean includeDeleted) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (videoServerId != null) p.add(cb.equal(root.get("videoServerId"), videoServerId));
            if (!includeDeleted) p.add(cb.isNull(root.get("deletedAt")));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (isActive != null) p.add(cb.equal(root.get("isActive"), isActive));
            if (q != null && !q.isBlank()) {
                String pattern = QuerySupport.containsPattern(q);
                p.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("externalId")), pattern, '\\'),
                        cb.like(cb.lower(root.get("code")), pattern, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}