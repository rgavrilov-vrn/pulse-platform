package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.videoserver.api.CreateVideoServerRequest;
import ru.agroeco.pulse.videoserver.api.VideoServerResponse;
import ru.agroeco.pulse.videoserver.domain.VideoServer;
import ru.agroeco.pulse.videoserver.domain.VideoServerStatus;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.VideoServerRepository;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateVideoServerUseCase {

    private final VideoServerRepository repository;

    @Transactional
    public VideoServerResponse handle(CreateVideoServerRequest request) {
        VideoServer entity = new VideoServer(
                UUID.randomUUID(),
                request.objectId(),
                request.zoneId(),
                request.source(),
                request.externalId(),
                request.name(),
                request.host(),
                request.baseUrl(),
                request.description(),
                parseStatus(request.status()),
                Boolean.TRUE.equals(request.isActive()) || request.isActive() == null
        );

        try {
            // saveAndFlush — чтобы constraint violation (uq_video_servers_source_external)
            // вылетел здесь, а не на commit за пределами try/catch
            VideoServer saved = repository.saveAndFlush(entity);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate video server: source={}, externalId={}",
                    request.source(), request.externalId());
            throw new BusinessRuleException(
                    "VIDEO_SERVER_ALREADY_EXISTS: source=" + request.source()
                            + ", externalId=" + request.externalId());
        }
    }

    private VideoServerStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return VideoServerStatus.UNKNOWN;
        }
        try {
            return VideoServerStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(
                    "INVALID_STATUS: '" + raw + "' (allowed: ONLINE, OFFLINE, UNKNOWN)");
        }
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