package ru.agroeco.pulse.incident.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCatalogChanged;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentCatalogRestoreServiceTest {
    private final EntityManager em=mock(EntityManager.class);
    private final EventDispatcher events=mock(EventDispatcher.class);
    private final IncidentCatalogRestoreService service=new IncidentCatalogRestoreService(em,events);
    private ViolationCategoryEntity category(boolean active) {
        var c=ViolationCategoryEntity.builder().id(UUID.randomUUID()).groupId(UUID.randomUUID()).code("CAT").name("Категория").active(active).build();
        when(em.find(ViolationCategoryEntity.class,c.getId(),LockModeType.PESSIMISTIC_WRITE)).thenReturn(c);
        when(em.find(CategoryGroupEntity.class,c.getGroupId())).thenReturn(CategoryGroupEntity.builder().id(c.getGroupId()).active(true).build());return c;
    }
    private ChecklistItemEntity point(ViolationCategoryEntity c) {
        var p=ChecklistItemEntity.builder().id(UUID.randomUUID()).categoryId(c.getId()).code("POINT").name("Точка").active(false).build();
        when(em.find(ChecklistItemEntity.class,p.getId())).thenReturn(p);return p;
    }
    @Test void restoresCategoryWithoutRestoringChildren() {
        var c=category(false);var actor=UUID.randomUUID();
        service.restoreCategory(c.getId(),actor);assertTrue(c.isActive());
        var capture=ArgumentCaptor.forClass(DomainEvent.class);verify(events).dispatch(capture.capture());
        var event=assertInstanceOf(IncidentCatalogChanged.class,capture.getValue());
        assertEquals(DomainEventType.INCIDENT_CATEGORY_RESTORED,event.eventType());
        assertEquals(false,assertInstanceOf(Map.class,event.oldDataSnapshot()).get("active"));
        assertEquals(true,assertInstanceOf(Map.class,event.newDataSnapshot()).get("active"));
        verify(em,never()).createQuery(anyString(),eq(ChecklistItemEntity.class));
    }
    @Test void restoresPointInActiveCategory() {
        var c=category(true);var p=point(c);service.restorePoint(p.getId(),UUID.randomUUID());
        assertTrue(p.isActive());verify(em).refresh(p,LockModeType.PESSIMISTIC_WRITE);verify(events).dispatch(any());
    }
    @Test void rejectsPointInDeletedCategory() {
        var c=category(false);var p=point(c);
        var error=assertThrows(ResponseStatusException.class,()->service.restorePoint(p.getId(),UUID.randomUUID()));
        assertEquals(409,error.getStatusCode().value());assertFalse(p.isActive());verifyNoInteractions(events);
    }
    @Test void repeatedRestoreDoesNotDuplicateAudit() {
        var c=category(true);service.restoreCategory(c.getId(),UUID.randomUUID());verifyNoInteractions(events);
    }
    @Test void inactiveGroupPreventsCategoryRestore() {
        var c=category(false);
        when(em.find(CategoryGroupEntity.class,c.getGroupId())).thenReturn(CategoryGroupEntity.builder().id(c.getGroupId()).active(false).build());
        assertThrows(ResponseStatusException.class,()->service.restoreCategory(c.getId(),UUID.randomUUID()));
        assertFalse(c.isActive());verifyNoInteractions(events);
    }
}
