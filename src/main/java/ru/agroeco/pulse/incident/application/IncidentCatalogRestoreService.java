package ru.agroeco.pulse.incident.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.adapter.http.dto.ChecklistItemAdminView;
import ru.agroeco.pulse.incident.adapter.http.dto.ViolationCategoryAdminView;
import ru.agroeco.pulse.incident.domain.CategoryGroupEntity;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;
import ru.agroeco.pulse.incident.domain.event.IncidentCatalogChanged;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentCatalogRestoreService {

    private final EntityManager em;
    private final EventDispatcher events;

    private ViolationCategoryEntity category(UUID id) {
        var c = em.find(ViolationCategoryEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена");
        return c;
    }

    private static Map<String, Object> snapshot(ViolationCategoryEntity c) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", c.getId()); m.put("groupId", c.getGroupId()); m.put("code", c.getCode());
        m.put("name", c.getName()); m.put("active", c.isActive()); return m;
    }

    private static Map<String, Object> snapshot(ChecklistItemEntity p) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", p.getId()); m.put("categoryId", p.getCategoryId());
        m.put("code", p.getCode()); m.put("name", p.getName());
        m.put("active", p.isActive()); return m;
    }

    @Transactional
    public ViolationCategoryAdminView restoreCategory(UUID id, UUID actor) {
        var c = category(id);
        if (c.isActive()) return ViolationCategoryAdminView.from(c);

        var group = em.find(CategoryGroupEntity.class, c.getGroupId());
        if (group == null || !group.isActive())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Нельзя восстановить категорию: тип события неактивен");

        var before = snapshot(c);
        c.setActive(true);
        em.flush();
        events.dispatch(new IncidentCatalogChanged(
                DomainEventType.INCIDENT_CATEGORY_RESTORED, "violation_categories",
                id, actor, Instant.now(), before, snapshot(c)));
        return ViolationCategoryAdminView.from(c);
    }

    @Transactional
    public ChecklistItemAdminView restorePoint(UUID id, UUID actor) {
        var p = em.find(ChecklistItemEntity.class, id);
        if (p == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Контрольная точка не найдена");
        var c = category(p.getCategoryId());
        if (!c.isActive())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Сначала восстановите категорию контрольной точки");
        em.refresh(p, LockModeType.PESSIMISTIC_WRITE);
        if (p.isActive()) return ChecklistItemAdminView.from(p);

        var before = snapshot(p);
        p.setActive(true);
        em.flush();
        events.dispatch(new IncidentCatalogChanged(
                DomainEventType.INCIDENT_CHECKLIST_ITEM_RESTORED, "checklist_items",
                id, actor, Instant.now(), before, snapshot(p)));
        return ChecklistItemAdminView.from(p);
    }
}