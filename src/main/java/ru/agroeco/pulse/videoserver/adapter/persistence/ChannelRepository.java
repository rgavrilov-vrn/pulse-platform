package ru.agroeco.pulse.videoserver.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.videoserver.domain.Channel;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ChannelRepository
        extends JpaRepository<Channel, UUID>,
        JpaSpecificationExecutor<Channel> {

    Optional<Channel> findByCameraIdAndExternalId(UUID cameraId, String externalId);

    /**
     * Soft-delete (is_active=false) каналов сервера, отсутствующих в snapshot.
     * Каналы ищутся через cameras.video_server_id.
     * НЕ выставляет deleted_at — в channels нет такого поля (is_active достаточно).
     */
    @Modifying
    @Query("""
            UPDATE Channel ch
               SET ch.isActive = false,
                   ch.updatedAt = :now
             WHERE ch.isActive = true
               AND ch.externalId NOT IN :seenKeys
               AND ch.cameraId IN (
                   SELECT c.id FROM Camera c WHERE c.videoServerId = :serverId
               )
            """)
    int softDeleteMissingForServer(@Param("serverId") UUID serverId,
                                   @Param("seenKeys") Set<String> seenKeys,
                                   @Param("now") Instant now);
}