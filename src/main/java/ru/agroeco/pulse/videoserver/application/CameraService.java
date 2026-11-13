package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.DuplicateEventException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.CameraResponse;
import ru.agroeco.pulse.videoserver.adapter.http.dto.CreateCameraRequest;
import ru.agroeco.pulse.videoserver.adapter.http.dto.UpdateCameraRequest;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CameraService {

    private final CameraRepository cameras;
    private final VideoServerRepository servers;

    @Transactional
    public CameraResponse create(CreateCameraRequest req) {
        VideoServer server = servers.findById(req.videoServerId())
                .orElseThrow(() -> new NotFoundException("Server not found: " + req.videoServerId()));
        if (cameras.findByVideoServerIdAndExternalId(server.getId(), req.externalId()).isPresent()) {
            throw new DuplicateEventException("Camera already exists: externalId=" + req.externalId());
        }
        Camera camera = new Camera(UUID.randomUUID(), server.getId(), server.getSource(),
                req.externalId(), req.code(), req.name(), req.description(), null, true);
        cameras.save(camera);
        return CameraResponse.from(camera);
    }

    @Transactional
    public CameraResponse update(UUID id, UpdateCameraRequest req) {
        Camera camera = cameras.findById(id)
                .orElseThrow(() -> new NotFoundException("Camera not found: " + id));
        if (camera.getDeletedAt() != null) {
            throw new BusinessRuleException("Cannot update deleted camera");
        }
        camera.updateManually(req.name(), req.code(), req.description());
        cameras.save(camera);
        return CameraResponse.from(camera);
    }

    @Transactional
    public void delete(UUID id) {
        Camera camera = cameras.findById(id)
                .orElseThrow(() -> new NotFoundException("Camera not found: " + id));
        if (camera.getDeletedAt() != null) return; // идемпотентно
        camera.softDeleteManually(Instant.now());
        cameras.save(camera);
    }
}