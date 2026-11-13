package ru.agroeco.pulse.videoserver.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import ru.agroeco.pulse.videoserver.adapter.http.dto.ChannelResponse;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.Channel;
import ru.agroeco.pulse.videoserver.domain.ChannelStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videoserver/channels")
@RequiredArgsConstructor
@Tag(name = "Video Servers", description = "Управление видеосерверами (VMS)")
public class ChannelController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "name", "externalId", "status", "isActive",
            "lastSeenAt", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final ChannelRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(
            summary = "List channels",
            description = "Фильтры: videoServerId (все каналы сервера — через join на cameras), "
                    + "cameraId (каналы конкретной камеры), q (поиск по name/externalId), "
                    + "status, isActive, includeInactive (default: false — только активные). "
                    + "Sort: name, externalId, status, isActive, lastSeenAt, createdAt, updatedAt, id.",
            parameters = {
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sorting. Allowed fields: name, externalId, status, "
                                    + "isActive, lastSeenAt, createdAt, updatedAt, id",
                            example = "name,ASC"),
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Page number (0-based)", example = "0"),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Page size (1..100)", example = "20")
            }
    )
    public PageResponse<ChannelResponse> list(
            @RequestParam(required = false) UUID videoServerId,
            @RequestParam(required = false) UUID cameraId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ChannelStatus status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        Specification<Channel> spec = buildSpec(videoServerId, cameraId, q, status, isActive, includeInactive);
        return PageResponse.of(repository.findAll(spec, safe), ChannelResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "Get channel by id")
    public ChannelResponse getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ChannelResponse::from)
                .orElseThrow(() -> new NotFoundException("Channel not found: " + id));
    }

    private Specification<Channel> buildSpec(UUID videoServerId, UUID cameraId, String q,
                                             ChannelStatus status, Boolean isActive,
                                             boolean includeInactive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (cameraId != null) {
                predicates.add(cb.equal(root.get("cameraId"), cameraId));
            }
            if (videoServerId != null) {
                // channel.cameraId IN (SELECT cam.id FROM Camera cam WHERE cam.videoServerId = :sid)
                Subquery<UUID> sq = query.subquery(UUID.class);
                Root<Camera> cam = sq.from(Camera.class);
                sq.select(cam.get("id"));
                sq.where(cb.equal(cam.get("videoServerId"), videoServerId));
                predicates.add(root.get("cameraId").in(sq));
            }

            // isActive (явный фильтр) имеет приоритет над includeInactive
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            } else if (!includeInactive) {
                predicates.add(cb.equal(root.get("isActive"), true));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String pattern = QuerySupport.containsPattern(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),       pattern, '\\'),
                        cb.like(cb.lower(root.get("externalId")), pattern, '\\')
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}