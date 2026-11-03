package ru.agroeco.pulse.incident.application;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.adapter.http.dto.UpdateIncidentRequest;
import ru.agroeco.pulse.incident.adapter.persistence.*;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IncidentCategoryChangeTest {
    private final ViolationCategoryRepository categories=mock(ViolationCategoryRepository.class);
    private final IncidentChecklistItemRepository links=mock(IncidentChecklistItemRepository.class);
    private final ChecklistItemRepository items=mock(ChecklistItemRepository.class);
    private final EventDispatcher events=mock(EventDispatcher.class);
    private final IncidentCategoryChange service=new IncidentCategoryChange(categories,links,items,events);
    private final UUID oldId=UUID.randomUUID(),newId=UUID.randomUUID(),actor=UUID.randomUUID();
    private IncidentEntity incident() {
        return IncidentEntity.builder().id(UUID.randomUUID()).categoryId(oldId).version(4).status(IncidentStatus.DRAFT)
                .metadata(Map.of("checkpoint","OLD","checklistItemName","Старая","siteName","Площадка")).build();
    }
    private IncidentChecklistItemEntity prepare(IncidentEntity i) {
        var link=IncidentChecklistItemEntity.builder().id(UUID.randomUUID()).incidentId(i.getId()).checklistItemId(UUID.randomUUID()).build();
        when(categories.findById(newId)).thenReturn(Optional.of(ViolationCategoryEntity.builder().id(newId).active(true).build()));
        when(links.findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(i.getId())).thenReturn(List.of(link));return link;
    }
    private UpdateIncidentRequest request(boolean confirm,List<UUID> ids,int version) {
        return new UpdateIncidentRequest(newId,null,null,null,null,null,null,confirm,oldId,version,ids);
    }
    @Test void withoutConfirmationLeavesLinksUntouched() {
        var i=incident();var link=prepare(i);
        assertThrows(BusinessRuleException.class,()->service.apply(i,request(false,List.of(link.getId()),4),actor));
        assertFalse(link.isDeleted());assertEquals(oldId,i.getCategoryId());verifyNoInteractions(events);
    }
    @Test void confirmationSoftDeletesLinksAndClearsLegacyMetadata() {
        var i=incident();var link=prepare(i);
        service.apply(i,request(true,List.of(link.getId()),4),actor);
        assertTrue(link.isDeleted());assertEquals(actor,link.getDeletedBy());assertNotNull(link.getDeletedAt());
        assertEquals(newId,i.getCategoryId());assertFalse(i.getMetadata().containsKey("checkpoint"));
        assertFalse(i.getMetadata().containsKey("checklistItemName"));assertEquals("Площадка",i.getMetadata().get("siteName"));
        verify(links).saveAllAndFlush(List.of(link));verify(events).dispatch(any());
    }
    @Test void changedLinkSetRequiresFreshConfirmation() {
        var i=incident();var link=prepare(i);
        var error=assertThrows(ResponseStatusException.class,()->service.apply(i,request(true,List.of(),4),actor));
        assertEquals(409,error.getStatusCode().value());assertFalse(link.isDeleted());verifyNoInteractions(events);
    }
    @Test void changedVersionRequiresFreshConfirmation() {
        var i=incident();var link=prepare(i);
        assertThrows(ResponseStatusException.class,()->service.apply(i,request(true,List.of(link.getId()),3),actor));
        assertFalse(link.isDeleted());verifyNoInteractions(events);
    }
    @Test void publishedRecordCannotUseReplacement() {
        var i=incident();i.setStatus(IncidentStatus.RECORDED);var link=prepare(i);
        assertThrows(BusinessRuleException.class,()->service.apply(i,request(true,List.of(link.getId()),4),actor));
        assertFalse(link.isDeleted());verifyNoInteractions(events);
    }
}
