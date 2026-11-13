package ru.agroeco.pulse.videoserver.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.api.event.TrassirObjectsSnapshotReceived;
import ru.agroeco.pulse.videoserver.adapter.persistence.CameraRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.ChannelRepository;
import ru.agroeco.pulse.videoserver.adapter.persistence.VideoServerRepository;
import ru.agroeco.pulse.videoserver.domain.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

/**
 * Reconciliation inventory из agent.objects_snapshot.
 *
 * <p>Структура объекта в envelope TRASSIR (v2.0, с enrichment):</p>
 * <pre>
 * {
 *   "class_name": "Channel" | "IP Device",
 *   "object_id": "A09M5rCx",
 *   "parent_id": "tkcbkSr1C",           // ВСЕГДА Folder, НЕ используем для parent
 *   "values": {
 *     "ip_device_id": "CKxYAfh6",       // для Channel: ссылка на IP Device
 *     "channel_ids": ["A09M5rCx"],      // для IP Device: обратные ссылки на Channel
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p><b>Стратегия разрешения parent:</b></p>
 * <ol>
 *   <li>{@code values.ip_device_id} — прямое поле в Channel (enriched snapshots)</li>
 *   <li>Обратная карта из {@code IP Device.values.channel_ids} → {@code IP Device.object_id}</li>
 *   <li>Никогда не используем top-level {@code parent_id} — в TRASSIR 2.0 это Folder</li>
 * </ol>
 *
 * <p><b>Snapshots без enrichment</b> (Channel без {@code values.ip_device_id} + IP Device
 * без {@code values.channel_ids}) — каналы пропускаются с WARN. Следующий snapshot с
 * enrichment их создаст.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileVideoServersInventoryUseCase {

    private static final String CLASS_IP_DEVICE = "IP Device";
    private static final String CLASS_CHANNEL   = "Channel";

    private final VideoServerRepository servers;
    private final CameraRepository cameras;
    private final ChannelRepository channels;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(TrassirObjectsSnapshotReceived event) {
        Optional<VideoServer> opt = servers.findById(event.serverId());
        if (opt.isEmpty()) {
            log.warn("Snapshot for unknown serverId={} — skipping (configure POST /servers first)",
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

        Instant seenAt = event.receivedAt();

        // =========================================================
        // 0. Pre-pass: разобрать все объекты, построить reverse-map
        //    channelObjectKey -> ipDeviceObjectKey (из IP Device.values.channel_ids)
        // =========================================================
        Map<String, JsonNode> payloadByKey = new HashMap<>();
        Map<String, String> ipDeviceByObjectKey = new HashMap<>();   // object_key -> class_name (IP Device)
        Map<String, String> channelToDevice = new HashMap<>();       // channel.object_id -> IP Device.object_id

        for (var obj : event.objects()) {
            if (obj.objectKey() == null || obj.objectKey().isBlank()) continue;
            JsonNode payload = parsePayload(obj.payloadJson());
            payloadByKey.put(obj.objectKey(), payload);

            String className = textOrNull(payload, "class_name");
            if (CLASS_IP_DEVICE.equals(className)) {
                ipDeviceByObjectKey.put(obj.objectKey(), className);

                // IP Device.values.channel_ids — обратные ссылки на Channel
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
            }
        }

        // =========================================================
        // 1. IP Device → Camera
        // =========================================================
        Set<String> seenCameraKeys = new HashSet<>();
        int createdCameras = 0, updatedCameras = 0;

        for (var obj : event.objects()) {
            JsonNode payload = payloadByKey.get(obj.objectKey());
            if (payload == null) continue;
            if (!CLASS_IP_DEVICE.equals(textOrNull(payload, "class_name"))) continue;

            seenCameraKeys.add(obj.objectKey());

            String name        = textOr(payload, "name", obj.objectKey());
            String code        = textOrNull(payload, "code");
            String description = textOrNull(payload, "description");

            var existing = cameras.findByVideoServerIdAndExternalId(event.serverId(), obj.objectKey());
            if (existing.isPresent()) {
                existing.get().applySnapshot(name, code, description, seenAt);
                cameras.save(existing.get());
                updatedCameras++;
            } else {
                Camera camera = new Camera(
                        UUID.randomUUID(), event.serverId(), "trassir", obj.objectKey(),
                        code, name, description, null, true);
                camera.applySnapshot(name, code, description, seenAt);
                cameras.save(camera);
                createdCameras++;
            }
        }
        cameras.flush();

        // =========================================================
        // 2. Channel → Channel (parent resolution)
        // =========================================================
        Set<String> seenChannelKeys = new HashSet<>();
        int createdChannels = 0, updatedChannels = 0, skippedChannels = 0;
        int resolvedViaValues = 0, resolvedViaReverseMap = 0;

        for (var obj : event.objects()) {
            JsonNode payload = payloadByKey.get(obj.objectKey());
            if (payload == null) continue;
            if (!CLASS_CHANNEL.equals(textOrNull(payload, "class_name"))) continue;

            // Resolve parent (IP Device object_key)
            ParentResolution parent = resolveChannelParent(obj.objectKey(), payload,
                    ipDeviceByObjectKey, channelToDevice);

            if (parent.key() == null) {
                log.warn("Channel {} has no resolvable IP Device (values.ip_device_id missing, "
                                + "no reverse-map entry from IP Device.values.channel_ids) — skipping. "
                                + "Will be created on next enriched snapshot.",
                        obj.objectKey());
                skippedChannels++;
                continue;
            }
            if (parent.via() == ParentResolution.Via.VALUES) resolvedViaValues++;
            else if (parent.via() == ParentResolution.Via.REVERSE_MAP) resolvedViaReverseMap++;

            var cameraOpt = cameras.findByVideoServerIdAndExternalId(event.serverId(), parent.key());
            if (cameraOpt.isEmpty()) {
                log.warn("Channel {} references IP Device {} which is not in cameras "
                                + "(device must be in same snapshot) — skipping",
                        obj.objectKey(), parent.key());
                skippedChannels++;
                continue;
            }
            UUID cameraId = cameraOpt.get().getId();
            seenChannelKeys.add(obj.objectKey());

            String name          = textOr(payload, "name", obj.objectKey());
            ChannelStatus status = parseStatus(textOrNull(payload, "status"));

            var existing = channels.findByCameraIdAndExternalId(cameraId, obj.objectKey());
            if (existing.isPresent()) {
                existing.get().applySnapshot(name, status, seenAt);
                channels.save(existing.get());
                updatedChannels++;
            } else {
                Channel channel = new Channel(
                        UUID.randomUUID(), cameraId, obj.objectKey(), name, status, true);
                channel.applySnapshot(name, status, seenAt);
                channels.save(channel);
                createdChannels++;
            }
        }

        // =========================================================
        // 3. Soft-delete
        // =========================================================
        int deactivatedCameras = 0, deactivatedChannels = 0;

        if (!seenCameraKeys.isEmpty()) {
            deactivatedCameras = cameras.softDeleteMissingForServer(
                    event.serverId(), seenCameraKeys, seenAt);
        } else if (!event.objects().isEmpty()) {
            log.warn("Snapshot for serverId={} contains no '{}' — skipping camera soft-delete",
                    event.serverId(), CLASS_IP_DEVICE);
        }

        if (!seenChannelKeys.isEmpty()) {
            deactivatedChannels = channels.softDeleteMissingForServer(
                    event.serverId(), seenChannelKeys, seenAt);
        } else if (skippedChannels > 0) {
            // Snapshot содержит Channel, но ни один не разрешён → не трогаем каналы,
            // чтобы не удалить существующие из-за неполного snapshot
            log.warn("Snapshot for serverId={} has {} channels but none resolvable — "
                            + "skipping channel soft-delete (incomplete enrichment)",
                    event.serverId(), skippedChannels);
        }

        log.info("Reconciled server={}: cameras(created={}, updated={}, deactivated={}), "
                        + "channels(created={}, updated={}, skipped={}, deactivated={}, "
                        + "via_values={}, via_reverse_map={})",
                event.serverId(),
                createdCameras, updatedCameras, deactivatedCameras,
                createdChannels, updatedChannels, skippedChannels, deactivatedChannels,
                resolvedViaValues, resolvedViaReverseMap);
    }

    // ---------------------------------------------------------------- helpers

    private record ParentResolution(String key, Via via) {
        enum Via { VALUES, REVERSE_MAP, NONE }
        static ParentResolution none() { return new ParentResolution(null, Via.NONE); }
    }

    /**
     * Разрешить parent (IP Device object_key) для Channel.
     * Порядок: values.ip_device_id → reverse-map из IP Device.values.channel_ids.
     * Top-level parent_id НЕ используем: в TRASSIR 2.0 он указывает на Folder.
     */
    private ParentResolution resolveChannelParent(String channelObjectKey,
                                                  JsonNode channelPayload,
                                                  Map<String, String> ipDeviceByObjectKey,
                                                  Map<String, String> channelToDevice) {
        // 1. values.ip_device_id
        JsonNode values = channelPayload.get("values");
        if (values != null && values.isObject()) {
            String v = textOrNull(values, "ip_device_id");
            if (v != null && ipDeviceByObjectKey.containsKey(v)) {
                return new ParentResolution(v, ParentResolution.Via.VALUES);
            }
            // альтернативные имена в values (на случай других версий агента)
            for (String alt : List.of("ip_device", "device_id", "camera_id")) {
                String altV = textOrNull(values, alt);
                if (altV != null && ipDeviceByObjectKey.containsKey(altV)) {
                    return new ParentResolution(altV, ParentResolution.Via.VALUES);
                }
            }
        }

        // 2. Reverse-map из IP Device.values.channel_ids
        String fromMap = channelToDevice.get(channelObjectKey);
        if (fromMap != null && ipDeviceByObjectKey.containsKey(fromMap)) {
            return new ParentResolution(fromMap, ParentResolution.Via.REVERSE_MAP);
        }

        return ParentResolution.none();
    }

    private ChannelStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ChannelStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ChannelStatus.UNKNOWN;
        }
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