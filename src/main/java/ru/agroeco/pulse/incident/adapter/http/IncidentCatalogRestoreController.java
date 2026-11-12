package ru.agroeco.pulse.incident.adapter.http;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemAdminView;
import ru.agroeco.pulse.incident.adapter.http.dto.ViolationCategoryAdminView;
import ru.agroeco.pulse.incident.application.IncidentCatalogRestoreService;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident/catalog-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('incident_catalog:manage')")
public class IncidentCatalogRestoreController {

    private final IncidentCatalogRestoreService service;

    @PostMapping("/categories/{id}/restore")
    public ViolationCategoryAdminView restoreCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.restoreCategory(id, actor.id());
    }

    @PostMapping("/checklist-items/{id}/restore")
    public ChecklistItemAdminView restorePoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        return service.restorePoint(id, actor.id());
    }
}