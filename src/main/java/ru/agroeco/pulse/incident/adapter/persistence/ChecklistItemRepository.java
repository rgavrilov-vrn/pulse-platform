package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.incident.domain.ChecklistItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItemEntity, UUID> {

    // Новые привязки разрешены только для активной точки активной категории.
    @Query("SELECT p FROM ChecklistItemEntity p WHERE p.code=:code AND p.active=true AND EXISTS (SELECT c.id FROM ViolationCategoryEntity c WHERE c.id=p.categoryId AND c.active=true)")
    Optional<ChecklistItemEntity> findByCode(@Param("code") String code);

    List<ChecklistItemEntity> findAllByActiveTrueOrderBySortOrderAscCodeAsc();

    List<ChecklistItemEntity>
    findAllByActiveTrueAndCategoryIdOrderBySortOrderAscCodeAsc(UUID categoryId);
}
