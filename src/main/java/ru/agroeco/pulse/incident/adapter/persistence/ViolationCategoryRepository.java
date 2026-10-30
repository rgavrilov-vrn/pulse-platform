package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.incident.domain.ViolationCategoryEntity;

import java.util.List;
import java.util.UUID;

public interface ViolationCategoryRepository
        extends JpaRepository<ViolationCategoryEntity, UUID> {

    /** Без фильтра: сортируем по группе, затем по sortOrder — «Событие» выше «Нарушения». */
    @Query(value = """
            SELECT c.* FROM incident.violation_categories c
            JOIN incident.category_groups g ON g.id = c.group_id
            WHERE c.is_active = TRUE
            ORDER BY g.sort_order, c.sort_order, c.code
            """, nativeQuery = true)
    List<ViolationCategoryEntity> findAllActiveOrdered();

    List<ViolationCategoryEntity>
    findAllByActiveTrueAndGroupIdOrderBySortOrderAscCodeAsc(UUID groupId);
}