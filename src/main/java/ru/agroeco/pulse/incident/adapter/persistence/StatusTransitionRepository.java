package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.StatusTransitionEntity;
import ru.agroeco.pulse.incident.domain.StatusTransitionId;

public interface StatusTransitionRepository extends JpaRepository<StatusTransitionEntity, StatusTransitionId> {
}