package ru.agroeco.pulse.incident.adapter.http;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemAdminView;
import ru.agroeco.pulse.incident.adapter.http.dto.DeleteCategoryResult;
import ru.agroeco.pulse.incident.adapter.http.dto.ViolationCategoryAdminView;
import ru.agroeco.pulse.incident.application.IncidentCatalogAdminService;
import ru.agroeco.pulse.incident.application.IncidentCatalogAdminService.CategoryInput;
import ru.agroeco.pulse.incident.application.IncidentCatalogAdminService.PointInput;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident/catalog-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('incident_catalog:read')")
public class IncidentCatalogAdminController {

    private final IncidentCatalogAdminService service;

    @GetMapping("/categories")
    public List<ViolationCategoryAdminView> categories(
            @RequestParam(required = false) UUID groupId,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return service.categories(groupId, includeDeleted);
    }

    @GetMapping("/checklist-items")
    public List<ChecklistItemAdminView> points(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return service.points(categoryId, includeDeleted);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public ViolationCategoryAdminView createCategory(
            @Valid @RequestBody CategoryInput body,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.createCategory(body, actor.id());
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public ViolationCategoryAdminView updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryInput body,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.updateCategory(id, body, actor.id());
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public DeleteCategoryResult deleteCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.deleteCategory(id, actor.id());
    }

    @PostMapping("/checklist-items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public ChecklistItemAdminView createPoint(
            @Valid @RequestBody PointInput body,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.createPoint(body, actor.id());
    }

    @PutMapping("/checklist-items/{id}")
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public ChecklistItemAdminView updatePoint(
            @PathVariable UUID id,
            @Valid @RequestBody PointInput body,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.updatePoint(id, body, actor.id());
    }

    @DeleteMapping("/checklist-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('incident_catalog:manage')")
    public void deletePoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        service.deletePoint(id, actor.id());
    }
}