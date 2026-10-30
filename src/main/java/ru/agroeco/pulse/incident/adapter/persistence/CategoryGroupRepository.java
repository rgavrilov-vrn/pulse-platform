package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.CategoryGroupEntity;

import java.util.List;
import java.util.UUID;

public interface CategoryGroupRepository extends JpaRepository<CategoryGroupEntity, UUID> {
    List<CategoryGroupEntity> findAllByActiveTrueOrderBySortOrderAscCodeAsc();
}