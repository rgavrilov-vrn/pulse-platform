package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.UpdateVideoServerRequest;
import ru.agroeco.pulse.videoserver.adapter.http.dto.VideoServerResponse;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.domain.VideoServer;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateVideoServerUseCase {

    private final VideoServerRepository repository;

    @Transactional
    public VideoServerResponse update(UUID id, UpdateVideoServerRequest req) {
        VideoServer server = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video server not found: " + id));
        if (server.getDeletedAt() != null) {
            throw new BusinessRuleException("Cannot update deleted server");
        }
        server.updateManually(req.name(), req.host(), req.baseUrl(), req.description(),
                req.objectId(), req.zoneId(), req.isActive());
        repository.save(server);
        return toResponse(server);
    }

    @Transactional
    public void delete(UUID id) {
        VideoServer server = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video server not found: " + id));
        if (server.getDeletedAt() != null) return;
        server.softDeleteManually(Instant.now());
        repository.save(server);
    }

    private VideoServerResponse toResponse(VideoServer s) {
        return new VideoServerResponse(
                s.getId(), s.getObjectId(), s.getZoneId(),
                s.getSource(), s.getExternalId(), s.getName(),
                s.getHost(), s.getBaseUrl(), s.getDescription(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}