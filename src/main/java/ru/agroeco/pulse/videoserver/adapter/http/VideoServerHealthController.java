package ru.agroeco.pulse.videoserver.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.videoserver.adapter.http.dto.ServerHealthView;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerHealthRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videoserver/servers/{id}/health")
@RequiredArgsConstructor
@Tag(name = "Video Servers")
public class VideoServerHealthController {

    private final VideoServerHealthRepository healthRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('video:read')")
    @Operation(summary = "Get server health (from last agent.objects_snapshot)")
    public ServerHealthView health(@PathVariable UUID id) {
        return healthRepository.findById(id)
                .map(ServerHealthView::from)
                .orElseThrow(() -> new NotFoundException("Server health not yet collected: " + id));
    }
}