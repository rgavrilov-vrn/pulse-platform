package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.api.event.TrassirObjectsSnapshotReceived;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.domain.Camera;
import ru.agroeco.pulse.videoserver.domain.VideoServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileVideoServersInventoryUseCase {

    private final VideoServerRepository servers;
    private final CameraRepository cameras;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TrassirObjectsSnapshotReceived event) {
        Optional<VideoServer> opt = servers.findById(event.serverId());
        if (opt.isEmpty()) {
            log.warn("Snapshot for unknown serverId={} — skipping (configure via POST /servers first)",
                    event.serverId());
            return;
        }
        VideoServer server = opt.get();
        if (!"trassir".equals(server.getSource())) {
            log.warn("Snapshot for serverId={} with source='{}' — skipping (expected 'trassir')",
                    event.serverId(), server.getSource());
            return;
        }

        server.markSeen(event.receivedAt());
        servers.saveAndFlush(server);

        Set<String> seenKeys = new HashSet<>();
        Instant seenAt = event.receivedAt();
        for (var obj : event.objects()) {
            if (obj.objectKey() == null || obj.objectKey().isBlank()) {
                log.warn("Snapshot object without objectKey on serverId={}, skipping", event.serverId());
                continue;
            }
            seenKeys.add(obj.objectKey());

            JsonNode payload = parsePayload(obj.payloadJson());
            String name = textOr(payload, "name", obj.objectKey());
            String code = textOrNull(payload, "code");
            String description = textOrNull(payload, "description");

            Camera camera = cameras
                    .findByVideoServerIdAndExternalId(event.serverId(), obj.objectKey())
                    .orElseGet(() -> new Camera(
                            UUID.randomUUID(), event.serverId(), "trassir", obj.objectKey(),
                            null, name, null, null, true));
            camera.applySnapshot(name, code, description, seenAt);
            cameras.save(camera);
        }

        int deactivated = cameras.softDeleteMissingForServer(event.serverId(), seenKeys, seenAt);
        log.info("Reconciled server={}: seen={}, deactivated={}",
                event.serverId(), seenKeys.size(), deactivated);
    }

    private JsonNode parsePayload(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Cannot parse snapshot payload: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String textOr(JsonNode n, String field, String fallback) {
        String v = textOrNull(n, field);
        return v == null ? fallback : v;
    }

    private String textOrNull(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }
}