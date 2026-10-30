package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.IncidentChecklistItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentChecklistItemRepository
        extends JpaRepository<IncidentChecklistItemEntity, UUID> {

    List<IncidentChecklistItemEntity>
    findByIncidentIdAndDeletedFalseOrderByCreatedAtDesc(UUID incidentId);

    Optional<IncidentChecklistItemEntity>
    findByIncidentIdAndChecklistItemIdAndDeletedFalse(UUID incidentId, UUID checklistItemId);

    boolean existsByIncidentIdAndChecklistItemIdAndDeletedFalse(
            UUID incidentId, UUID checklistItemId);
}