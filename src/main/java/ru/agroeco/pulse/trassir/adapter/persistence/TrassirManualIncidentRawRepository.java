package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.trassir.domain.TrassirManualIncidentRaw;

import java.util.UUID;

public interface TrassirManualIncidentRawRepository extends JpaRepository<TrassirManualIncidentRaw, UUID> {
    boolean existsByEventId(UUID eventId);
}