package ru.agroeco.pulse.videoserver.api;

import java.util.Optional;
import java.util.UUID;

public interface CameraLookupService {
    record ObjectInfo(
            UUID cameraId,
            UUID channelId,
            String name,
            UUID videoServerId,
            String videoServerBaseUrl
    ) {}

    Optional<ObjectInfo> findByExternalId(String externalId);
}
