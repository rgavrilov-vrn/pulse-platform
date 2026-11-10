package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.incident.domain.IncidentContextRef;
import ru.agroeco.pulse.incident.domain.IncidentContextRefId;

import java.util.UUID;

public interface IncidentContextRefRepository
        extends JpaRepository<IncidentContextRef, IncidentContextRefId> {

    /**
     * FIX #67: bulk-delete через @Modifying @Query. Раньше derived-метод
     * deleteByIncidentId требовал активной транзакции у вызывающего,
     * что было неявным. Теперь это обычный bulk-DML.
     * ВАЖНО: вызывать из @Transactional-контекста.
     */
    @Modifying
    @Query("delete from IncidentContextRef cr where cr.incidentId = :incidentId")
    int deleteByIncidentId(@Param("incidentId") UUID incidentId);
}