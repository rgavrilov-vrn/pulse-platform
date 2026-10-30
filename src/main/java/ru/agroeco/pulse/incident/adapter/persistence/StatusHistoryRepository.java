package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.StatusHistoryEntity;

import java.util.UUID;

public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, Long> {

    Page<StatusHistoryEntity> findByIncidentId(UUID incidentId, Pageable pageable);
}