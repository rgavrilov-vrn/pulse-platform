package ru.agroeco.pulse.refs.api;

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
import ru.agroeco.pulse.refs.application.CreateZoneUseCase;
import ru.agroeco.pulse.refs.domain.Zone;
import ru.agroeco.pulse.refs.infrastructure.persistence.ZoneRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ref/zones")
@RequiredArgsConstructor
@Tag(name = "Reference · Zones", description = "Справочник зон")
public class ZoneController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "code", "name", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CreateZoneUseCase createUseCase;
    private final ZoneRepository repository;

    @PostMapping
    @PreAuthorize("hasAuthority('ref:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create zone",
            description = "(objectId, code) уникально. parentId должен принадлежать тому же objectId.")
    public ZoneResponse create(@Valid @RequestBody CreateZoneRequest request) {
        return createUseCase.handle(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ref:read')")
    @Operation(summary = "List zones",
            description = "Фильтры: objectId, parentId, rootOnly (parent_id IS NULL), "
                    + "code (exact), search (по name, substring), isActive.")
    public PageResponse<ZoneResponse> list(
            @RequestParam(required = false) UUID objectId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) Boolean rootOnly,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        return PageResponse.of(
                repository.findAll(buildSpec(objectId, parentId, rootOnly, code, search, isActive), safe),
                this::toResponse);
    }

    private Specification<Zone> buildSpec(UUID objectId, UUID parentId, Boolean rootOnly,
                                          String code, String search, Boolean isActive) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (objectId != null) {
                p.add(cb.equal(root.get("objectId"), objectId));
            }
            if (Boolean.TRUE.equals(rootOnly)) {
                p.add(cb.isNull(root.get("parentId")));
            } else if (parentId != null) {
                p.add(cb.equal(root.get("parentId"), parentId));
            }
            if (code != null && !code.isBlank()) {
                p.add(cb.equal(root.get("code"), code));
            }
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")),
                        QuerySupport.containsPattern(search), '\\'));
            }
            if (isActive != null) {
                p.add(cb.equal(root.get("isActive"), isActive));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private ZoneResponse toResponse(Zone z) {
        return new ZoneResponse(
                z.getId(), z.getObjectId(), z.getParentId(),
                z.getCode(), z.getName(), z.getDescription(),
                z.isActive(), z.getCreatedAt(), z.getUpdatedAt());
    }
}