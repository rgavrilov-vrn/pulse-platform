package ru.agroeco.pulse.refs.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.refs.api.support.QuerySupport;
import ru.agroeco.pulse.refs.application.CreateFacilityUseCase;
import ru.agroeco.pulse.refs.domain.Facility;
import ru.agroeco.pulse.refs.infrastructure.persistence.FacilityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ref/facilities")
@RequiredArgsConstructor
@Tag(name = "Reference", description = "Справочник объектов (facilities)")
public class FacilityController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "code", "name", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CreateFacilityUseCase createUseCase;
    private final FacilityRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create facility",
            description = "code уникален. Дубликат → 422 FACILITY_CODE_ALREADY_EXISTS.")
    public FacilityResponse create(@Valid @RequestBody CreateFacilityRequest request) {
        return createUseCase.handle(request);
    }

    @GetMapping
    @Operation(summary = "List facilities",
            description = "Фильтры: code (exact), search (по name, substring), isActive.")
    public Page<FacilityResponse> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        return repository.findAll(buildSpec(code, search, isActive), safe).map(this::toResponse);
    }

    private Specification<Facility> buildSpec(String code, String search, Boolean isActive) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
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

    private FacilityResponse toResponse(Facility f) {
        return new FacilityResponse(
                f.getId(), f.getCode(), f.getName(),
                f.getDescription(), f.getAddress(),
                f.isActive(), f.getCreatedAt(), f.getUpdatedAt());
    }
}