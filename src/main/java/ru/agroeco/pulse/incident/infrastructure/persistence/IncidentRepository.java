package ru.agroeco.pulse.incident.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.Incident;

import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
}