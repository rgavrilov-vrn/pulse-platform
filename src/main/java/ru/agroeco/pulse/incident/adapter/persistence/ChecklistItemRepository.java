package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;

import java.util.List;
import java.util.UUID;

public interface ChecklistItemRepository
        extends JpaRepository<ChecklistItemEntity, UUID> {

    List<ChecklistItemEntity> findAllByActiveTrueOrderBySortOrderAscCodeAsc();

    List<ChecklistItemEntity>
    findAllByActiveTrueAndCategoryIdOrderBySortOrderAscCodeAsc(UUID categoryId);
}