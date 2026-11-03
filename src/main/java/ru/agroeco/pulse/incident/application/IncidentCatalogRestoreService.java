package ru.agroeco.pulse.incident.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.domain.CategoryGroupEntity;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;
import ru.agroeco.pulse.incident.domain.event.IncidentCatalogChanged;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentCatalogRestoreService {
    private final EntityManager em;
    private final EventDispatcher events;
    private ViolationCategoryEntity category(UUID id) {
        var c=em.find(ViolationCategoryEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(c==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Категория не найдена");
        return c;
    }
    private static Map<String,Object> snapshot(ViolationCategoryEntity c) {
        return Map.of("id",c.getId(),"groupId",c.getGroupId(),"code",c.getCode(),"name",c.getName(),"active",c.isActive());
    }
    private static Map<String,Object> snapshot(ChecklistItemEntity p) {
        return Map.of("id",p.getId(),"categoryId",p.getCategoryId(),"code",p.getCode(),"name",p.getName(),"active",p.isActive());
    }
    @Transactional
    public ViolationCategoryEntity restoreCategory(UUID id,UUID actor) {
        var c=category(id);
        if(c.isActive()) return c;
        var group=em.find(CategoryGroupEntity.class,c.getGroupId());
        if(group==null || !group.isActive())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Нельзя восстановить категорию: тип события неактивен");
        var before=snapshot(c);c.setActive(true);em.flush();
        events.dispatch(new IncidentCatalogChanged(DomainEventType.INCIDENT_CATEGORY_RESTORED,
                "violation_categories",id,actor,Instant.now(),before,snapshot(c)));
        return c;
    }
    @Transactional
    public ChecklistItemEntity restorePoint(UUID id,UUID actor) {
        var p=em.find(ChecklistItemEntity.class,id);
        if(p==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Контрольная точка не найдена");
        var c=category(p.getCategoryId());
        if(!c.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Сначала восстановите категорию контрольной точки");
        em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
        if(p.isActive()) return p;
        var before=snapshot(p);p.setActive(true);em.flush();
        events.dispatch(new IncidentCatalogChanged(DomainEventType.INCIDENT_CHECKLIST_ITEM_RESTORED,
                "checklist_items",id,actor,Instant.now(),before,snapshot(p)));
        return p;
    }
}
