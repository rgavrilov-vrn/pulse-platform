package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.api.CameraLookupService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CameraLookupServiceImpl implements CameraLookupService {

    private final CameraRepository cameraRepository;
    private final ChannelRepository channelRepository;
    private final VideoServerRepository videoServerRepository;

    @Override
    public Optional<ObjectInfo> findByExternalId(String externalId) {
        // 1. Ищем камеру
        var cam = cameraRepository.findByExternalId(externalId);
        if (cam.isPresent()) {
            return Optional.of(toObjectInfo(
                    cam.get().getId(),
                    null,
                    cam.get().getName(),
                    cam.get().getVideoServerId()
            ));
        }

        // 2. Ищем канал
        var ch = channelRepository.findByExternalId(externalId);
        if (ch.isPresent()) {
            var camera = cameraRepository.findById(ch.get().getCameraId());
            if (camera.isPresent()) {
                return Optional.of(toObjectInfo(
                        camera.get().getId(),
                        ch.get().getId(),
                        ch.get().getName(),
                        camera.get().getVideoServerId()
                ));
            }
        }

        return Optional.empty();
    }

    private ObjectInfo toObjectInfo(
            UUID cameraId,
            UUID channelId,
            String name,
            UUID videoServerId
    ) {
        String baseUrl = videoServerId == null
                ? null
                : videoServerRepository.findById(videoServerId)
                        .map(this::resolveBaseUrl)
                        .orElse(null);

        return new ObjectInfo(cameraId, channelId, name, videoServerId, baseUrl);
    }

    private String resolveBaseUrl(VideoServer videoServer) {
        if (videoServer.getBaseUrl() != null && !videoServer.getBaseUrl().isBlank()) {
            return videoServer.getBaseUrl();
        }

        if (videoServer.getHost() != null && !videoServer.getHost().isBlank()) {
            String host = videoServer.getHost();
            return host.startsWith("http://") || host.startsWith("https://")
                    ? host
                    : "https://" + host;
        }

        return null;
    }
}
