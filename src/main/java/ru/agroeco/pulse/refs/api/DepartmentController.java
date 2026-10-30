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
import ru.agroeco.pulse.refs.application.CreateDepartmentUseCase;
import ru.agroeco.pulse.refs.domain.Department;
import ru.agroeco.pulse.refs.infrastructure.persistence.DepartmentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/ref/departments")
@RequiredArgsConstructor
@Tag(name = "Reference · Departments", description = "Справочник подразделений")
public class DepartmentController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "code", "name", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "name";

    private final CreateDepartmentUseCase createUseCase;
    private final DepartmentRepository repository;

    @PostMapping
    @PreAuthorize("hasAuthority('ref:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create department")
    public DepartmentResponse create(@Valid @RequestBody CreateDepartmentRequest request) {
        return createUseCase.handle(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ref:read')")
    @Operation(summary = "List departments",
            description = "Фильтры: code (exact), search (по name, substring), isActive.")
    public PageResponse<DepartmentResponse> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        return PageResponse.of(repository.findAll(buildSpec(code, search, isActive), safe), this::toResponse);
    }

    private Specification<Department> buildSpec(String code, String search, Boolean isActive) {
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

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
                d.getId(), d.getCode(), d.getName(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}