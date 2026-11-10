package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirIngestRaw;

import java.util.UUID;

public interface TrassirIngestRawRepository extends JpaRepository<TrassirIngestRaw, UUID> {

    boolean existsByBatchId(UUID batchId);
}
