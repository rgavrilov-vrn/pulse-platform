package ru.agroeco.pulse.incident.adapter.http;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemAdminView;
import ru.agroeco.pulse.incident.adapter.http.dto.ViolationCategoryAdminView;
import ru.agroeco.pulse.incident.application.IncidentCatalogRestoreService;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incident/catalog-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('incident_catalog:manage')")
public class IncidentCatalogRestoreController {

    private final IncidentCatalogRestoreService service;

    /**
     * FIX #59: возвращаем DTO (ViolationCategoryAdminView), а не JPA entity.
     * Иначе наружу утекают все поля сущности, а контракт API привязан к ORM-модели.
     */
    @PostMapping("/categories/{id}/restore")
    public ViolationCategoryAdminView restoreCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        return toView(service.restoreCategory(id, actor.id()));
    }

    /**
     * FIX #59: возвращаем ChecklistItemAdminView вместо ChecklistItemEntity.
     */
    @PostMapping("/checklist-items/{id}/restore")
    public ChecklistItemAdminView restorePoint(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal actor) {
        return toView(service.restorePoint(id, actor.id()));
    }

    // ------------------------------------------------------------- mappers

    private static ViolationCategoryAdminView toView(ViolationCategoryEntity c) {
        return new ViolationCategoryAdminView(
                c.getId(),
                c.getGroupId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                c.getSortOrder(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    private static ChecklistItemAdminView toView(ChecklistItemEntity p) {
        return new ChecklistItemAdminView(
                p.getId(),
                p.getCategoryId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.isRequiresImage(),
                p.isRequiresComment(),
                p.isRequiresVideo(),
                p.getDefaultSeverity() != null ? p.getDefaultSeverity().name() : null,
                p.getCommentTemplate(),
                p.getSortOrder(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}