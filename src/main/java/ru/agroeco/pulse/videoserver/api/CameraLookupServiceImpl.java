package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.api.CameraLookupService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CameraLookupServiceImpl implements CameraLookupService {

    private final CameraRepository cameraRepository;
    private final ChannelRepository channelRepository;

    @Override
    public Optional<ObjectInfo> findByExternalId(String externalId) {
        // 1. Ищем камеру
        var cam = cameraRepository.findByExternalId(externalId);
        if (cam.isPresent()) {
            return Optional.of(new ObjectInfo(cam.get().getId(), null, cam.get().getName(), cam.get().getVideoServerId()));
        }
        // 2. Ищем канал
        var ch = channelRepository.findByExternalId(externalId);
        if (ch.isPresent()) {
            var camera = cameraRepository.findById(ch.get().getCameraId());
            if (camera.isPresent()) {
                // Возвращаем ID камеры и ID канала
                return Optional.of(new ObjectInfo(camera.get().getId(), ch.get().getId(), ch.get().getName(), camera.get().getVideoServerId()));
            }
        }
        return Optional.empty();
    }
}