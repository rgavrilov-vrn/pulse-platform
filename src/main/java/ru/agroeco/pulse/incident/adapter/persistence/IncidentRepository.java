package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.incident.domain.IncidentEntity;

import java.util.UUID;

public interface IncidentRepository extends
        JpaRepository<IncidentEntity, UUID>,
        JpaSpecificationExecutor<IncidentEntity> {
}