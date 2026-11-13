package ru.agroeco.pulse.videoserver.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.videoserver.domain.Camera;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CameraRepository
        extends JpaRepository<Camera, UUID>,
        JpaSpecificationExecutor<Camera> {

    Optional<Camera> findByVideoServerIdAndExternalId(UUID videoServerId, String externalId);

    /**
     * Soft-delete камер сервера, отсутствующих в snapshot.
     * НЕ трогает камеры с deactivated_by='manual' (админ отключил вручную).
     */
    @Modifying
    @Query("""
            UPDATE Camera c
               SET c.deletedAt = :now,
                   c.deactivatedBy = 'agent_snapshot',
                   c.isActive = false,
                   c.updatedAt = :now
             WHERE c.videoServerId = :serverId
               AND c.deletedAt IS NULL
               AND (c.deactivatedBy IS NULL OR c.deactivatedBy <> 'manual')
               AND c.externalId NOT IN :seenKeys
            """)
    int softDeleteMissingForServer(@Param("serverId") UUID serverId,
                                   @Param("seenKeys") Set<String> seenKeys,
                                   @Param("now") Instant now);
}