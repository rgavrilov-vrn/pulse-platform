package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.DuplicateEventException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.ChannelResponse;
import ru.agroeco.pulse.videoserver.adapter.http.dto.CreateChannelRequest;
import ru.agroeco.pulse.videoserver.adapter.http.dto.UpdateChannelRequest;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.Channel;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channels;
    private final CameraRepository cameras;

    @Transactional
    public ChannelResponse create(CreateChannelRequest req) {
        Camera camera = cameras.findById(req.cameraId())
                .orElseThrow(() -> new NotFoundException("Camera not found: " + req.cameraId()));
        if (channels.findByCameraIdAndExternalId(camera.getId(), req.externalId()).isPresent()) {
            throw new DuplicateEventException("Channel already exists: externalId=" + req.externalId());
        }
        Channel channel = new Channel(UUID.randomUUID(), camera.getId(), req.externalId(), req.name(), null, true);
        channels.save(channel);
        return ChannelResponse.from(channel);
    }

    @Transactional
    public ChannelResponse update(UUID id, UpdateChannelRequest req) {
        Channel channel = channels.findById(id)
                .orElseThrow(() -> new NotFoundException("Channel not found: " + id));
        if (!channel.isActive()) {
            throw new BusinessRuleException("Cannot update inactive channel");
        }
        channel.updateManually(req.name(), channel.getStatus());
        channels.save(channel);
        return ChannelResponse.from(channel);
    }

    @Transactional
    public void delete(UUID id) {
        Channel channel = channels.findById(id)
                .orElseThrow(() -> new NotFoundException("Channel not found: " + id));
        if (!channel.isActive()) return;
        channel.softDeleteManually(Instant.now());
        channels.save(channel);
    }
}