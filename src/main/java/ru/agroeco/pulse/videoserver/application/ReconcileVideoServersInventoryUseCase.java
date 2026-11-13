package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.trassir.api.event.TrassirObjectsSnapshotReceived;
import ru.agroeco.pulse.videoserver.adapter.persistence.*;
import ru.agroeco.pulse.videoserver.domain.*;
import ru.agroeco.pulse.videoserver.domain.event.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileVideoServersInventoryUseCase {

    private static final String CLASS_IP_DEVICE = "IP Device";
    private static final String CLASS_CHANNEL   = "Channel";
    private static final String CLASS_SERVER    = "Server";

    private final VideoServerRepository servers;
    private final CameraRepository cameras;
    private final ChannelRepository channels;
    private final VideoServerHealthRepository healths;
    private final EventDispatcher events;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TrassirObjectsSnapshotReceived event) {
        Optional<VideoServer> serverOpt = servers.findById(event.serverId());
        if (serverOpt.isEmpty()) {
            log.warn("Snapshot for unknown serverId={} — skipping", event.serverId());
            return;
        }
        VideoServer server = serverOpt.get();
        if (!"trassir".equals(server.getSource())) {
            log.warn("Snapshot for serverId={} source='{}' — skipping", event.serverId(), server.getSource());
            return;
        }

        server.markSeen(event.receivedAt());
        servers.saveAndFlush(server);

        Instant seenAt = event.receivedAt();

        // 0. Парсинг + reverse-map
        Map<String, JsonNode> payloadByKey = new HashMap<>();
        Map<String, String> ipDeviceByObjectKey = new HashMap<>();
        Map<String, String> channelToDevice = new HashMap<>();
        JsonNode serverPayload = null;

        for (var obj : event.objects()) {
            if (obj.objectKey() == null || obj.objectKey().isBlank()) continue;
            JsonNode payload = parsePayload(obj.payloadJson());
            payloadByKey.put(obj.objectKey(), payload);
            String className = textOrNull(payload, "class_name");
            if (CLASS_IP_DEVICE.equals(className)) {
                ipDeviceByObjectKey.put(obj.objectKey(), className);
                JsonNode values = payload.get("values");
                if (values != null && values.isObject()) {
                    JsonNode chIds = values.get("channel_ids");
                    if (chIds != null && chIds.isArray()) {
                        for (JsonNode chId : chIds) {
                            if (chId.isString() && !chId.asString().isBlank()) {
                                channelToDevice.put(chId.asString(), obj.objectKey());
                            }
                        }
                    }
                }
            } else if (CLASS_SERVER.equals(className)) {
                serverPayload = payload;
            }
        }

        // 1. server_health
        if (serverPayload != null) {
            reconcileServerHealth(event.serverId(), serverPayload, seenAt);
        }

        // 2. IP Device → Camera (с diff-событиями)
        Set<String> seenCameraKeys = new HashSet<>();
        int createdCameras = 0, updatedCameras = 0;
        int camOffline = 0, camOnline = 0, camDegraded = 0, camRecovered = 0;

        for (var obj : event.objects()) {
            JsonNode payload = payloadByKey.get(obj.objectKey());
            if (payload == null) continue;
            if (!CLASS_IP_DEVICE.equals(textOrNull(payload, "class_name"))) continue;

            seenCameraKeys.add(obj.objectKey());

            JsonNode v = payload.get("values");
            String name = textOr(payload, "name", obj.objectKey());
            String code = textOrNull(payload, "code");
            String description = textOrNull(payload, "description");
            String connection = v != null ? textOrNull(v, "connection") : null;
            String ipAddress  = v != null ? textOrNull(v, "connection_ip") : null;
            String model      = v != null ? textOrNull(v, "model") : null;
            String family     = v != null ? textOrNull(v, "family") : null;

            var existingOpt = cameras.findByVideoServerIdAndExternalId(event.serverId(), obj.objectKey());
            Camera camera;
            Camera.CameraSnapshotDiff diff;
            boolean isNew = existingOpt.isEmpty();

            if (isNew) {
                camera = new Camera(UUID.randomUUID(), event.serverId(), "trassir", obj.objectKey(),
                        code, name, description, null, true);
                diff = camera.applySnapshotWithDiff(name, code, description, seenAt,
                        connection, ipAddress, model, family);
                cameras.save(camera);
                createdCameras++;
            } else {
                camera = existingOpt.get();
                diff = camera.applySnapshotWithDiff(name, code, description, seenAt,
                        connection, ipAddress, model, family);
                cameras.save(camera);
                updatedCameras++;
            }

            // derived events — только для существующих камер (не для первой встречи)
            if (!isNew) {
                if (diff.isOffline() && !"Disconnected".equalsIgnoreCase(diff.previousConnection())) {
                    events.dispatch(CameraOffline.builder()
                            .cameraId(camera.getId()).videoServerId(event.serverId())
                            .externalId(camera.getExternalId()).cameraName(camera.getName())
                            .connectionStatus(connection).lastSeenAt(seenAt)
                            .occurredAt(seenAt).build());
                    camOffline++;
                } else if (diff.isRecoveryTransition()) {
                    events.dispatch(CameraOnline.builder()
                            .cameraId(camera.getId()).videoServerId(event.serverId())
                            .externalId(camera.getExternalId()).cameraName(camera.getName())
                            .lastSeenAt(seenAt).occurredAt(seenAt).build());
                    camOnline++;
                } else if (diff.isDegradedTransition()) {
                    events.dispatch(CameraDegraded.builder()
                            .cameraId(camera.getId()).videoServerId(event.serverId())
                            .externalId(camera.getExternalId()).cameraName(camera.getName())
                            .previousStatus(diff.previousConnection()).currentStatus(connection)
                            .occurredAt(seenAt).build());
                    camDegraded++;
                }
            }
        }
        cameras.flush();

        // 3. Channel → Channel (с diff-событиями)
        Set<String> seenChannelKeys = new HashSet<>();
        int createdChannels = 0, updatedChannels = 0, skippedChannels = 0;
        int streamLost = 0, streamRestored = 0, recordingStopped = 0;

        for (var obj : event.objects()) {
            JsonNode payload = payloadByKey.get(obj.objectKey());
            if (payload == null) continue;
            if (!CLASS_CHANNEL.equals(textOrNull(payload, "class_name"))) continue;

            JsonNode v = payload.get("values");
            String parentKey = v != null ? textOrNull(v, "ip_device_id") : null;
            if (parentKey == null) parentKey = channelToDevice.get(obj.objectKey());

            if (parentKey == null) {
                log.warn("Channel {} has no resolvable IP Device — skipping", obj.objectKey());
                skippedChannels++;
                continue;
            }
            var cameraOpt = cameras.findByVideoServerIdAndExternalId(event.serverId(), parentKey);
            if (cameraOpt.isEmpty()) { skippedChannels++; continue; }

            UUID cameraId = cameraOpt.get().getId();
            seenChannelKeys.add(obj.objectKey());

            String name = textOr(payload, "name", obj.objectKey());
            ChannelStatus status = parseStatus(textOrNull(payload, "status"));
            Boolean signal = v != null && v.has("signal") ? v.get("signal").asInt() != 0 : null;
            Boolean recordingLocal = v != null && v.has("recording_local") ? v.get("recording_local").asInt() != 0 : null;
            String grabberError = v != null ? textOrNull(v, "grabber_error") : null;
            Float fpsMain = v != null && v.has("fps_main") ? (float) v.get("fps_main").asDouble() : null;
            Float kbpsMain = v != null && v.has("kbps_main") ? (float) v.get("kbps_main").asDouble() : null;

            var existingOpt = channels.findByCameraIdAndExternalId(cameraId, obj.objectKey());
            Channel channel;
            Channel.ChannelSnapshotDiff diff;
            boolean isNew = existingOpt.isEmpty();

            if (isNew) {
                channel = new Channel(UUID.randomUUID(), cameraId, obj.objectKey(), name, status, true);
                diff = channel.applySnapshotWithDiff(name, status, seenAt,
                        signal, recordingLocal, grabberError, fpsMain, kbpsMain);
                channels.save(channel);
                createdChannels++;
            } else {
                channel = existingOpt.get();
                diff = channel.applySnapshotWithDiff(name, status, seenAt,
                        signal, recordingLocal, grabberError, fpsMain, kbpsMain);
                channels.save(channel);
                updatedChannels++;
            }

            if (!isNew) {
                if (diff.isStreamLost()) {
                    events.dispatch(ChannelStreamLost.builder()
                            .channelId(channel.getId()).cameraId(cameraId).videoServerId(event.serverId())
                            .channelExternalId(channel.getExternalId()).channelName(channel.getName())
                            .occurredAt(seenAt).build());
                    streamLost++;
                }
                if (diff.isStreamRestored()) {
                    events.dispatch(ChannelStreamRestored.builder()
                            .channelId(channel.getId()).cameraId(cameraId).videoServerId(event.serverId())
                            .channelExternalId(channel.getExternalId()).channelName(channel.getName())
                            .occurredAt(seenAt).build());
                    streamRestored++;
                }
                if (diff.isRecordingStopped()) {
                    events.dispatch(ChannelRecordingStopped.builder()
                            .channelId(channel.getId()).cameraId(cameraId).videoServerId(event.serverId())
                            .channelExternalId(channel.getExternalId()).channelName(channel.getName())
                            .occurredAt(seenAt).build());
                    recordingStopped++;
                }
            }
        }

        // 4. soft-delete
        int deactivatedCameras = 0, deactivatedChannels = 0;
        if (!seenCameraKeys.isEmpty()) {
            deactivatedCameras = cameras.softDeleteMissingForServer(event.serverId(), seenCameraKeys, seenAt);
        }
        if (!seenChannelKeys.isEmpty()) {
            deactivatedChannels = channels.softDeleteMissingForServer(event.serverId(), seenChannelKeys, seenAt);
        }

        log.info("Reconciled server={}: cameras(created={},updated={},deactivated={}), "
                        + "channels(created={},updated={},skipped={},deactivated={}), "
                        + "derived(cam_offline={},cam_online={},cam_degraded={},stream_lost={},stream_restored={},rec_stopped={})",
                event.serverId(),
                createdCameras, updatedCameras, deactivatedCameras,
                createdChannels, updatedChannels, skippedChannels, deactivatedChannels,
                camOffline, camOnline, camDegraded, streamLost, streamRestored, recordingStopped);
    }

    // ---------------------------------------------------------------- helpers

    private void reconcileServerHealth(UUID serverId, JsonNode serverPayload, Instant seenAt) {
        JsonNode values = serverPayload.get("values");
        if (values == null || !values.isObject()) return;

        VideoServerHealth health = healths.findById(serverId)
                .orElseGet(() -> VideoServerHealth.forServer(serverId));

        Map<String, Object> raw = objectMapper.convertValue(values, Map.class);

        health.apply(seenAt,
                textOrNull(serverPayload, "name"),
                parseMicroseconds(textOrNull(values, "startup_ts")),
                boolValue(values, "server_ok"),
                floatValue(values, "cpu_usage"),
                floatValue(values, "gpu_usage"),
                boolValue(values, "db_connected"),
                boolValue(values, "db_is_slow"),
                textOrNull(values, "db_last_error"),
                intValue(values, "scripts_ok"),
                intValue(values, "scripts_total"),
                intValue(values, "disks_error_count"),
                boolValue(values, "disks_is_slow"),
                textOrNull(values, "disks_last_error"),
                floatValue(values, "disks_stat_main_days"),
                floatValue(values, "disks_stat_subs_days"),
                intValue(values, "ip_devices_total"),
                intValue(values, "ip_devices_connected"),
                intValue(values, "channels_network_total"),
                intValue(values, "channels_network_online"),
                boolValue(values, "network_should_be_connected"),
                boolValue(values, "network_really_connected"),
                textOrNull(values, "network_last_error"),
                raw);
        healths.save(health);
    }

    private ChannelStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return ChannelStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return ChannelStatus.UNKNOWN; }
    }

    private JsonNode parsePayload(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { return objectMapper.createObjectNode(); }
    }

    private String textOr(JsonNode n, String field, String fallback) {
        String v = textOrNull(n, field); return v == null ? fallback : v;
    }

    private String textOrNull(JsonNode n, String field) {
        if (n == null) return null;
        JsonNode v = n.get(field);
        return v != null && v.isString() && !v.asString().isBlank() ? v.asString() : null;
    }

    private Boolean boolValue(JsonNode n, String f) {
        if (n == null) return null;
        JsonNode v = n.get(f);
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isInt()) return v.asInt() != 0;
        return null;
    }

    private Float floatValue(JsonNode n, String f) {
        if (n == null) return null;
        JsonNode v = n.get(f);
        return v != null && v.isNumber() ? (float) v.asDouble() : null;
    }

    private Integer intValue(JsonNode n, String f) {
        if (n == null) return null;
        JsonNode v = n.get(f);
        return v != null && v.isNumber() ? v.asInt() : null;
    }

    private Instant parseMicroseconds(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            long micros = Long.parseLong(raw.trim());
            if (micros <= 0) return null;
            return Instant.ofEpochSecond(micros / 1_000_000L, (micros % 1_000_000L) * 1_000L);
        } catch (Exception e) { return null; }
    }
}