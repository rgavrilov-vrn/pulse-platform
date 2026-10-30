package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItemEntity, UUID> {

    // для привязки чек-листа к инциденту по коду
    Optional<ChecklistItemEntity> findByCode(String code);

    // существующие методы справочной ручки
    List<ChecklistItemEntity> findAllByActiveTrueOrderBySortOrderAscCodeAsc();

    List<ChecklistItemEntity>
    findAllByActiveTrueAndCategoryIdOrderBySortOrderAscCodeAsc(UUID categoryId);
}