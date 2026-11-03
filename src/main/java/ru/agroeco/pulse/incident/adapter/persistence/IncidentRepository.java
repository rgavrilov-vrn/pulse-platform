package ru.agroeco.pulse.incident.adapter.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.incident.domain.IncidentEntity;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends
        JpaRepository<IncidentEntity, UUID>,
        JpaSpecificationExecutor<IncidentEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IncidentEntity i WHERE i.id=:id")
    Optional<IncidentEntity> findByIdForUpdate(@Param("id") UUID id);
}
