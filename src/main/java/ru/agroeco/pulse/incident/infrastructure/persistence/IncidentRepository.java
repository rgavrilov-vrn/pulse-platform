package ru.agroeco.pulse.incident.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.Incident;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);
}
