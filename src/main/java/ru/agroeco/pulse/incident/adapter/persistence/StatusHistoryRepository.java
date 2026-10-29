package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.StatusHistoryEntity;

public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, Long> {
}