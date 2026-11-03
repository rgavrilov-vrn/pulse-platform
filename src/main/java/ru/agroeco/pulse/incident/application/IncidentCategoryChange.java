package ru.agroeco.pulse.incident.application;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.adapter.http.dto.UpdateIncidentRequest;
import ru.agroeco.pulse.incident.adapter.persistence.*;
import ru.agroeco.pulse.incident.domain.*;
import ru.agroeco.pulse.incident.domain.event.IncidentCheckpointRemoved;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IncidentCategoryChange {
    private final ViolationCategoryRepository categories;
    private final IncidentChecklistItemRepository links;
    private final ChecklistItemRepository items;
    private final EventDispatcher events;

    @Transactional(propagation=Propagation.MANDATORY)
    public void apply(IncidentEntity incident, UpdateIncidentRequest req, UUID actor) {
        var target=categories.findById(req.categoryId()).filter(ViolationCategoryEntity::isActive)
                .orElseThrow(()->new BusinessRuleException("Категория не найдена или неактивна"));
        var current=links.findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(incident.getId());
        boolean confirmed=Boolean.TRUE.equals(req.replaceCheckpoints());
        if(confirmed) {
            if(incident.getStatus()!=IncidentStatus.DRAFT)
                throw new BusinessRuleException("Подтверждаемая смена категории доступна только в черновике");
            var expected=req.expectedCheckpointIds();
            var actual=new HashSet<>(current.stream().map(IncidentChecklistItemEntity::getId).toList());
            if(req.expectedVersion()==null || !Objects.equals(req.expectedVersion(),incident.getVersion())
                    || !Objects.equals(req.expectedCategoryId(),incident.getCategoryId()) || expected==null
                    || expected.stream().anyMatch(Objects::isNull) || expected.size()!=new HashSet<>(expected).size()
                    || !actual.equals(new HashSet<>(expected)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Карточка или привязки изменились. Обновите карточку и подтвердите смену категории заново.");
        } else {
            boolean foreign=current.stream().anyMatch(link->items.findById(link.getChecklistItemId())
                    .map(item->!target.getId().equals(item.getCategoryId())).orElse(true));
            boolean legacy=incident.getMetadata()!=null && List.of("checkpoint","checklistItemId","checklistItemName").stream()
                    .anyMatch(key->incident.getMetadata().get(key)!=null && !incident.getMetadata().get(key).toString().isBlank());
            if(foreign || legacy) throw new BusinessRuleException("Подтвердите смену категории и снятие прежних привязок контрольных точек");
        }
        if(confirmed && !current.isEmpty()) {
            var now=Instant.now();
            for(var link:current) {
                link.setDeleted(true);link.setDeletedAt(now);link.setDeletedBy(actor);
            }
            links.saveAllAndFlush(current);
            for(var link:current) events.dispatch(IncidentCheckpointRemoved.builder()
                    .incidentId(incident.getId()).linkId(link.getId()).checklistItemId(link.getChecklistItemId())
                    .actorId(actor).occurredAt(now).build());
        }
        var metadata=new HashMap<String,Object>();
        if(incident.getMetadata()!=null) metadata.putAll(incident.getMetadata());
        metadata.remove("checkpoint");metadata.remove("checklistItemId");metadata.remove("checklistItemName");
        incident.setMetadata(metadata);
        incident.setCategoryId(target.getId());
    }
}
