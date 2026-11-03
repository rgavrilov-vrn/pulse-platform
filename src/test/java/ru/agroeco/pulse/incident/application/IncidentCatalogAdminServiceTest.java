package ru.agroeco.pulse.incident.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;
import ru.agroeco.pulse.incident.domain.event.IncidentCatalogChanged;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentCatalogAdminServiceTest {
    private final EntityManager em=mock(EntityManager.class);
    private final EventDispatcher events=mock(EventDispatcher.class);
    private final IncidentCatalogAdminService service=new IncidentCatalogAdminService(em,events);
    @Test void categoryDeleteDeactivatesChildrenAndAuditsTheirIds() {
        var id=UUID.randomUUID();var actor=UUID.randomUUID();
        var c=ViolationCategoryEntity.builder().id(id).groupId(UUID.randomUUID()).code("CAT").name("Категория").active(true).build();
        var p=ChecklistItemEntity.builder().id(UUID.randomUUID()).categoryId(id).active(true).build();
        when(em.find(ViolationCategoryEntity.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        @SuppressWarnings("unchecked")
        TypedQuery<ChecklistItemEntity> q=mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery("SELECT p FROM ChecklistItemEntity p WHERE p.categoryId=:id AND p.active=true",ChecklistItemEntity.class)).thenReturn(q);
        when(q.getResultList()).thenReturn(List.of(p));
        var result=service.deleteCategory(id,actor);
        assertFalse(c.isActive());assertFalse(p.isActive());
        assertEquals(List.of(p.getId()),result.get("deletedCheckpointIds"));
        var captured=ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).dispatch(captured.capture());
        var event=assertInstanceOf(IncidentCatalogChanged.class,captured.getValue());
        assertEquals(DomainEventType.INCIDENT_CATEGORY_DELETED,event.eventType());
        assertEquals(actor,event.actorId());assertFalse(event.publishToOutbox());
        verify(em,never()).remove(any());
    }
    @Test void repeatedCategoryDeleteIsIdempotent() {
        var id=UUID.randomUUID();
        var c=ViolationCategoryEntity.builder().id(id).active(false).build();
        when(em.find(ViolationCategoryEntity.class,id,LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        @SuppressWarnings("unchecked")
        TypedQuery<ChecklistItemEntity> q=mock(TypedQuery.class,RETURNS_SELF);
        when(em.createQuery("SELECT p FROM ChecklistItemEntity p WHERE p.categoryId=:id AND p.active=true",ChecklistItemEntity.class)).thenReturn(q);
        when(q.getResultList()).thenReturn(List.of());
        assertEquals(0,service.deleteCategory(id,UUID.randomUUID()).get("deletedCheckpointCount"));
        verifyNoInteractions(events);
    }
}
