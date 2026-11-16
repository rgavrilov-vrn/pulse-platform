package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.api.CameraLookupService;
import ru.agroeco.pulse.videoserver.domain.VideoServer;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CameraLookupServiceImpl implements CameraLookupService {
    private final CameraRepository cameraRepository;
    private final ChannelRepository channelRepository;
    private final VideoServerRepository videoServerRepository;

    @Override
    public Optional<ObjectInfo> findByExternalId(String externalId) {
        var camera = cameraRepository.findByExternalId(externalId);
        if (camera.isPresent()) {
            var c = camera.get();
            return Optional.of(info(c.getId(), null, c.getName(), c.getVideoServerId()));
        }
        var channel = channelRepository.findByExternalId(externalId);
        if (channel.isEmpty()) return Optional.empty();
        var ch = channel.get();
        return cameraRepository.findById(ch.getCameraId())
                .map(c -> info(c.getId(), ch.getId(), ch.getName(), c.getVideoServerId()));
    }

    private ObjectInfo info(UUID cameraId, UUID channelId, String name, UUID serverId) {
        String url = serverId == null ? null : videoServerRepository.findById(serverId)
                .map(this::baseUrl).orElse(null);
        return new ObjectInfo(cameraId, channelId, name, serverId, url);
    }

    private String baseUrl(VideoServer server) {
        String value = server.getBaseUrl();
        if (value == null || value.isBlank()) value = server.getHost();
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        return value.startsWith("https://") || value.startsWith("http://")
                ? value : "https://" + value;
    }
}
