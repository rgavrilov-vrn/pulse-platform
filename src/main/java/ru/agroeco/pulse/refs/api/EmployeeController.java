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
import ru.agroeco.pulse.refs.application.CreateEmployeeUseCase;
import ru.agroeco.pulse.refs.domain.Employee;
import ru.agroeco.pulse.refs.infrastructure.persistence.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ref/employees")
@RequiredArgsConstructor
@Tag(name = "Reference · Employees", description = "Справочник сотрудников")
public class EmployeeController {

    private static final Set<String> ALLOWED_SORT = Set.of(
            "personnelNo", "fullName", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT = "fullName";

    private final CreateEmployeeUseCase createUseCase;
    private final EmployeeRepository repository;

    @PostMapping
    @PreAuthorize("hasAuthority('ref:write')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create employee",
            description = "personnelNo nullable, но unique. departmentId должен существовать.")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return createUseCase.handle(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ref:read')")
    @Operation(summary = "List employees",
            description = "Фильтры: departmentId, personnelNo (exact), "
                    + "search (по fullName, substring), isActive.")
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String personnelNo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = DEFAULT_SORT, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safe = QuerySupport.sanitize(pageable, ALLOWED_SORT, DEFAULT_SORT, Sort.Direction.ASC);
        return PageResponse.of(
                repository.findAll(buildSpec(departmentId, personnelNo, search, isActive), safe),
                this::toResponse);
    }

    private Specification<Employee> buildSpec(UUID departmentId, String personnelNo,
                                              String search, Boolean isActive) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (departmentId != null) {
                p.add(cb.equal(root.get("departmentId"), departmentId));
            }
            if (personnelNo != null && !personnelNo.isBlank()) {
                p.add(cb.equal(root.get("personnelNo"), personnelNo));
            }
            if (search != null && !search.isBlank()) {
                p.add(cb.like(cb.lower(root.get("fullName")),
                        QuerySupport.containsPattern(search), '\\'));
            }
            if (isActive != null) {
                p.add(cb.equal(root.get("isActive"), isActive));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getPersonnelNo(), e.getFullName(),
                e.getDepartmentId(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}