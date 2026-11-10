package ru.agroeco.pulse.incident.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCatalogChanged;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class IncidentCatalogAdminService {
    private final EntityManager em;
    private final EventDispatcher events;
    public record CategoryInput(@NotNull UUID groupId,
            @NotBlank @Size(max=100) String code,@NotBlank @Size(max=255) String name,
            @Size(max=10000) String description,@Min(0) int sortOrder) {}
    public record PointInput(
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 500) String name,
            @Size(max = 10000) String description,
            @Size(max = 10000) String commentTemplate,
            @NotNull IncidentSeverity defaultSeverity,   // было String
            @Min(0) int sortOrder,
            boolean requiresImage, boolean requiresComment, boolean requiresVideo) {}

    public List<ViolationCategoryEntity> categories(UUID groupId,boolean includeDeleted) {
        var q=em.createQuery("SELECT c FROM ViolationCategoryEntity c WHERE 1=1"
                +(includeDeleted?"":" AND c.active=true")+(groupId==null?"":" AND c.groupId=:groupId")
                +" ORDER BY c.sortOrder,c.code",ViolationCategoryEntity.class);
        if(groupId!=null) q.setParameter("groupId",groupId);
        return q.getResultList();
    }
    public List<ChecklistItemEntity> points(UUID categoryId,boolean includeDeleted) {
        var q=em.createQuery("SELECT p FROM ChecklistItemEntity p WHERE 1=1"
                +(includeDeleted?"":" AND p.active=true")+(categoryId==null?"":" AND p.categoryId=:categoryId")
                +" ORDER BY p.sortOrder,p.code",ChecklistItemEntity.class);
        if(categoryId!=null) q.setParameter("categoryId",categoryId);
        return q.getResultList();
    }
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT,message);
    }
    private ViolationCategoryEntity category(UUID id) {
        var c=em.find(ViolationCategoryEntity.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(c==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Категория не найдена");
        return c;
    }
    private static void active(ViolationCategoryEntity c) {
        if(!c.isActive()) throw conflict("Категория удалена");
    }
    private ChecklistItemEntity point(UUID id) {
        var p=em.find(ChecklistItemEntity.class,id);
        if(p==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Контрольная точка не найдена");
        return p;
    }
    private void unique(String entity,String code) {
        Long count=em.createQuery("SELECT count(e) FROM "+entity+" e WHERE e.code=:code",Long.class)
                .setParameter("code",code).getSingleResult();
        if(count>0) throw conflict("Код уже используется, в том числе удалённой записью");
    }
    private void flush() {
        try { em.flush(); } catch(RuntimeException ex) {
            for(Throwable cause=ex;cause!=null;cause=cause.getCause())
                if(cause instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState()))
                    throw conflict("Код уже используется");
            throw ex;
        }
    }
    private void audit(DomainEventType type,String table,UUID id,UUID actor,Object before,Object after) {
        events.dispatch(new IncidentCatalogChanged(type,table,id,actor,Instant.now(),before,after));
    }
    private static Map<String,Object> snapshot(ViolationCategoryEntity c) {
        var m=new LinkedHashMap<String,Object>();
        m.put("id",c.getId());m.put("groupId",c.getGroupId());m.put("code",c.getCode());
        m.put("name",c.getName());m.put("description",c.getDescription());
        m.put("sortOrder",c.getSortOrder());m.put("active",c.isActive());return m;
    }
    private static Map<String,Object> snapshot(ChecklistItemEntity p) {
        var m=new LinkedHashMap<String,Object>();
        m.put("id",p.getId());m.put("categoryId",p.getCategoryId());m.put("code",p.getCode());
        m.put("name",p.getName());m.put("description",p.getDescription());
        m.put("commentTemplate",p.getCommentTemplate());m.put("defaultSeverity",p.getDefaultSeverity());
        m.put("sortOrder",p.getSortOrder());m.put("active",p.isActive());
        m.put("requiresImage",p.isRequiresImage());m.put("requiresComment",p.isRequiresComment());
        m.put("requiresVideo",p.isRequiresVideo());return m;
    }
    private static void apply(ViolationCategoryEntity c,CategoryInput input) {
        c.setName(input.name().trim());c.setDescription(input.description());c.setSortOrder(input.sortOrder());
    }
    private static void apply(ChecklistItemEntity p,PointInput input) {
        p.setName(input.name().trim());p.setDescription(input.description());p.setCommentTemplate(input.commentTemplate());
        p.setDefaultSeverity(input.defaultSeverity());p.setSortOrder(input.sortOrder());
        p.setRequiresImage(input.requiresImage());p.setRequiresComment(input.requiresComment());p.setRequiresVideo(input.requiresVideo());
    }
    @Transactional
    public ViolationCategoryEntity createCategory(CategoryInput input,UUID actor) {
        var group=em.find(CategoryGroupEntity.class,input.groupId());
        if(group==null || !group.isActive()) throw conflict("Тип события не найден или неактивен");
        unique("ViolationCategoryEntity",input.code().trim());
        var c=new ViolationCategoryEntity();c.setId(UUID.randomUUID());c.setGroupId(input.groupId());
        c.setCode(input.code().trim());c.setActive(true);apply(c,input);em.persist(c);flush();
        audit(DomainEventType.INCIDENT_CATEGORY_CREATED,"violation_categories",c.getId(),actor,null,snapshot(c));return c;
    }
    @Transactional
    public ViolationCategoryEntity updateCategory(UUID id,CategoryInput input,UUID actor) {
        var c=category(id);active(c);
        if(!c.getCode().equals(input.code().trim()) || !c.getGroupId().equals(input.groupId()))
            throw conflict("Код и тип категории после создания не изменяются");
        var before=snapshot(c);apply(c,input);flush();
        audit(DomainEventType.INCIDENT_CATEGORY_UPDATED,"violation_categories",id,actor,before,snapshot(c));return c;
    }
    @Transactional
    public Map<String,Object> deleteCategory(UUID id,UUID actor) {
        var c=category(id);var before=snapshot(c);
        var children=em.createQuery("SELECT p FROM ChecklistItemEntity p WHERE p.categoryId=:id AND p.active=true",ChecklistItemEntity.class)
                .setParameter("id",id).getResultList();
        var affected=children.stream().map(ChecklistItemEntity::getId).toList();
        for(var p:children) p.setActive(false);
        boolean changed=c.isActive() || !children.isEmpty();c.setActive(false);flush();
        var after=snapshot(c);after.put("deletedCheckpointIds",affected);after.put("deletedCheckpointCount",affected.size());
        if(changed) audit(DomainEventType.INCIDENT_CATEGORY_DELETED,"violation_categories",id,actor,before,after);
        return after;
    }
    @Transactional
    public ChecklistItemEntity createPoint(PointInput input,UUID actor) {
        active(category(input.categoryId()));unique("ChecklistItemEntity",input.code().trim());
        var p=new ChecklistItemEntity();p.setId(UUID.randomUUID());p.setCategoryId(input.categoryId());
        p.setCode(input.code().trim());p.setActive(true);apply(p,input);em.persist(p);flush();
        audit(DomainEventType.INCIDENT_CHECKLIST_ITEM_CREATED,"checklist_items",p.getId(),actor,null,snapshot(p));return p;
    }
    @Transactional
    public ChecklistItemEntity updatePoint(UUID id,PointInput input,UUID actor) {
        var p=point(id);active(category(p.getCategoryId()));
        if(!p.isActive()) throw conflict("Контрольная точка удалена");
        if(!p.getCode().equals(input.code().trim()) || !p.getCategoryId().equals(input.categoryId()))
            throw conflict("Код и категория контрольной точки после создания не изменяются");
        em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
        if(!p.isActive()) throw conflict("Контрольная точка удалена");
        var before=snapshot(p);apply(p,input);flush();
        audit(DomainEventType.INCIDENT_CHECKLIST_ITEM_UPDATED,"checklist_items",id,actor,before,snapshot(p));return p;
    }
    @Transactional
    public void deletePoint(UUID id,UUID actor) {
        var p=point(id);category(p.getCategoryId());em.refresh(p,LockModeType.PESSIMISTIC_WRITE);
        if(!p.isActive()) return;
        var before=snapshot(p);p.setActive(false);flush();
        audit(DomainEventType.INCIDENT_CHECKLIST_ITEM_DELETED,"checklist_items",id,actor,before,snapshot(p));
    }
}
