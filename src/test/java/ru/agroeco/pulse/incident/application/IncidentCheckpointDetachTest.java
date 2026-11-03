package ru.agroeco.pulse.incident.application;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.agroeco.pulse.incident.adapter.persistence.*;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCheckpointRemoved;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentCheckpointDetachTest {
    private final IncidentRepository incidents=mock(IncidentRepository.class);
    private final IncidentChecklistItemRepository links=mock(IncidentChecklistItemRepository.class);
    private final ChecklistItemRepository items=mock(ChecklistItemRepository.class);
    private final EventDispatcher events=mock(EventDispatcher.class);
    private final IncidentService service=new IncidentService(incidents,null,null,null,null,events,links,items,null,null,null,null,null);
    private final UUID id=UUID.randomUUID(),linkId=UUID.randomUUID(),itemId=UUID.randomUUID(),actor=UUID.randomUUID();
    private IncidentEntity prepare(Map<String,Object> metadata) {
        var incident=IncidentEntity.builder().id(id).status(IncidentStatus.DRAFT).metadata(metadata).build();
        var link=IncidentChecklistItemEntity.builder().id(linkId).incidentId(id).checklistItemId(itemId).itemNameSnapshot("Точка").build();
        when(incidents.findByIdForUpdate(id)).thenReturn(Optional.of(incident));
        when(links.findById(linkId)).thenReturn(Optional.of(link));return incident;
    }
    @Test void detachesOneLinkClearsMatchingLegacyAndKeepsOtherMetadata() {
        var incident=prepare(Map.of("checklistItemId",itemId.toString(),"checkpoint","POINT","checklistItemName","Точка","siteName","Площадка"));
        service.removeCheckpoint(id,linkId,actor);
        var link=links.findById(linkId).orElseThrow();
        assertTrue(link.isDeleted());assertEquals(actor,link.getDeletedBy());assertNotNull(link.getDeletedAt());
        assertEquals(Map.of("siteName","Площадка"),incident.getMetadata());assertEquals(actor,incident.getUpdatedBy());
        var capture=ArgumentCaptor.forClass(DomainEvent.class);verify(events).dispatch(capture.capture());
        assertEquals(linkId,assertInstanceOf(IncidentCheckpointRemoved.class,capture.getValue()).linkId());
        verify(items,never()).delete(any());
    }
    @Test void legacyDataOfAnotherPointIsPreserved() {
        var metadata=Map.<String,Object>of("checklistItemId",UUID.randomUUID().toString(),"checkpoint","OTHER");
        var incident=prepare(metadata);service.removeCheckpoint(id,linkId,actor);assertEquals(metadata,incident.getMetadata());
    }
    @Test void matchingLegacyCodeIsCleared() {
        var incident=prepare(Map.of("checkpoint","POINT","checklistItemName","Точка"));
        when(items.findById(itemId)).thenReturn(Optional.of(ChecklistItemEntity.builder().id(itemId).code("POINT").build()));
        service.removeCheckpoint(id,linkId,actor);assertTrue(incident.getMetadata().isEmpty());
    }
    @Test void publishedIncidentCannotBeDetached() {
        var incident=prepare(Map.of());incident.setStatus(IncidentStatus.RECORDED);
        assertThrows(BusinessRuleException.class,()->service.removeCheckpoint(id,linkId,actor));verifyNoInteractions(events);
        verify(links,never()).saveAndFlush(any());
    }
    @Test void cannotDetachAnotherIncidentsLink() {
        prepare(Map.of());
        when(links.findById(linkId)).thenReturn(Optional.of(IncidentChecklistItemEntity.builder().id(linkId).incidentId(UUID.randomUUID()).checklistItemId(itemId).build()));
        assertThrows(NotFoundException.class,()->service.removeCheckpoint(id,linkId,actor));verifyNoInteractions(events);
    }
}
