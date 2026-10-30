package ru.agroeco.pulse.incident.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.incident.adapter.http.dto.CategoryGroupView;
import ru.agroeco.pulse.incident.adapter.http.dto.CategoryView;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemView;
import ru.agroeco.pulse.incident.application.IncidentCatalogService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident")
@RequiredArgsConstructor
@Tag(name = "Incident Catalog", description = "Справочники домена инцидентов")
public class IncidentCatalogController {

    private final IncidentCatalogService catalog;

    @GetMapping("/category-groups")
    @PreAuthorize("hasAuthority('incident_catalog:read')")
    @Operation(summary = "List category groups",
            description = "Событие / Нарушение. Только активные, сортировка по sortOrder.")
    public List<CategoryGroupView> listGroups() {
        return catalog.listGroups();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('incident_catalog:read')")
    @Operation(summary = "List violation categories",
            description = "Только активные. Без groupId — все, сгруппированные порядком группы. "
                    + "С groupId — только категории этой группы.")
    public List<CategoryView> listCategories(
            @RequestParam(required = false) UUID groupId) {
        return catalog.listCategories(groupId);
    }

    @GetMapping("/checklist-items")
    @PreAuthorize("hasAuthority('incident_catalog:read')")
    @Operation(summary = "List checklist items",
            description = "Только активные. С categoryId — только пункты этой категории.")
    public List<ChecklistItemView> listChecklistItems(
            @RequestParam(required = false) UUID categoryId) {
        return catalog.listChecklistItems(categoryId);
    }
}