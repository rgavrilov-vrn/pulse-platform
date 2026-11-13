package ru.agroeco.pulse.videoserver.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.CameraResponse;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.CameraStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videoserver/cameras")
@RequiredArgsConstructor
@Tag(name = "Video Servers", description = "Управление видеосерверами (VMS)")
public class CameraController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "name", "externalId", "code", "status", "isActive",
            "lastSeenAt", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CameraRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(
            summary = "List cameras",
            description = "Фильтры: videoServerId, q (поиск по name/externalId/code), "
                    + "status, isActive, includeDeleted (default: false — только не удалённые). "
                    + "Sort: name, externalId, code, status, isActive, lastSeenAt, createdAt, updatedAt, id.",
            parameters = {
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sorting. Allowed fields: name, externalId, code, status, "
                                    + "isActive, lastSeenAt, createdAt, updatedAt, id",
                            example = "name,ASC"),
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Page number (0-based)", example = "0"),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Page size (1..100)", example = "20")
            }
    )
    public PageResponse<CameraResponse> list(
            @RequestParam(required = false) UUID videoServerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CameraStatus status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        Specification<Camera> spec = buildSpec(videoServerId, q, status, isActive, includeDeleted);
        return PageResponse.of(repository.findAll(spec, safe), CameraResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "Get camera by id")
    public CameraResponse getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(CameraResponse::from)
                .orElseThrow(() -> new NotFoundException("Camera not found: " + id));
    }

    private Specification<Camera> buildSpec(UUID videoServerId, String q, CameraStatus status,
                                            Boolean isActive, boolean includeDeleted) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (videoServerId != null) {
                predicates.add(cb.equal(root.get("videoServerId"), videoServerId));
            }
            if (!includeDeleted) {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (q != null && !q.isBlank()) {
                String pattern = QuerySupport.containsPattern(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),       pattern, '\\'),
                        cb.like(cb.lower(root.get("externalId")), pattern, '\\'),
                        cb.like(cb.lower(root.get("code")),       pattern, '\\')
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}