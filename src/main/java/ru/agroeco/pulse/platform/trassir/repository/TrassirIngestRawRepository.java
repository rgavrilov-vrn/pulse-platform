package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.platform.trassir.domain.TrassirIngestRaw;

import java.time.Instant;
import java.util.UUID;

public interface TrassirIngestRawRepository extends JpaRepository<TrassirIngestRaw, UUID> {

    boolean existsByBatchId(UUID batchId);

    @Modifying
    @Query("delete from TrassirIngestRaw raw where raw.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from TrassirIngestRaw raw")
    int deleteAllRaw();
}
