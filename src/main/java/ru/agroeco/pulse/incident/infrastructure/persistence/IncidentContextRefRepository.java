package ru.agroeco.pulse.incident.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.IncidentContextRef;
import ru.agroeco.pulse.incident.domain.IncidentContextRefId;

import java.util.UUID;

public interface IncidentContextRefRepository
        extends JpaRepository<IncidentContextRef, IncidentContextRefId> {

    void deleteByIncidentId(UUID incidentId);
}