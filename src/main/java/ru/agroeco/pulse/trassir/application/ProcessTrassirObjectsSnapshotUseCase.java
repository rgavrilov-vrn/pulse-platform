package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.domain.*;
import ru.agroeco.pulse.trassir.repository.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import ru.agroeco.pulse.trassir.domain.TrassirServerState;
import ru.agroeco.pulse.trassir.repository.TrassirServerStateRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTrassirObjectsSnapshotUseCase implements TrassirEventUseCase {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotPartRepository partRepository;
    private final TrassirSnapshotStreamRepository streamRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final TrassirObjectRepository objectRepository;
    private final ObjectMapper objectMapper;

    // добавь поле в класс:
    private final TrassirServerStateRepository serverStateRepository;

    @Value("${pulse.trassir.snapshot-retry.base-delay-seconds:60}")
    private long retryBaseDelaySeconds;

    @Override
    @Transactional
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();

        String snapshotIdText = text(data, "snapshot_id");
        Integer totalParts = intValue(data, "parts_total");
        Integer partIndex = intValue(data, "part_index");

        if (snapshotIdText == null || totalParts == null || partIndex == null) {
            return EventProcessingResult.failure(
                    "Snapshot data.snapshot_id, parts_total and part_index are required");
        }

        UUID externalSnapshotId;
        try {
            externalSnapshotId = UUID.fromString(snapshotIdText);
        } catch (IllegalArgumentException e) {
            return EventProcessingResult.failure("Snapshot data.snapshot_id must be a valid UUID");
        }

        // FIX #27, #28, #30: атомарное создание (stream_id = server_id) + FOR UPDATE.
        snapshotRepository.insertIfAbsent(
                UUID.randomUUID(), externalSnapshotId, context.serverId(),
                totalParts, context.event().observedAt());

        TrassirSnapshot snapshot = snapshotRepository
                .findByServerIdAndExternalSnapshotId(context.serverId(), externalSnapshotId)
                .orElseThrow(() -> new IllegalStateException(
                        "Snapshot vanished after insertIfAbsent: " + externalSnapshotId));

        if (snapshot.getCompletedAt() != null) {
            return EventProcessingResult.success();
        }

        // FIX #28: атомарная вставка part'а
        partRepository.insertIfAbsent(
                UUID.randomUUID(), snapshot.getId(), partIndex,
                toJson(context.event().data()), context.event().observedAt());

        if (partRepository.countBySnapshotId(snapshot.getId()) < totalParts) {
            return EventProcessingResult.success();   // ждём остальные parts
        }

        EventProcessingResult result = assembleInternal(snapshot);

        // FIX #16: markFailed фиксируется в той же транзакции,
        // НЕ откатывается — use case не бросает.
        if (!result.successful()) {
            snapshotRepository.markFailed(
                    snapshot.getId(), result.error(),
                    Instant.now().plusSeconds(retryBaseDelaySeconds));
        }
        return result;
    }

    /**
     * FIX #29: публичный метод для retry-worker. Блокирует snapshot FOR UPDATE,
     * выполняет сборку. Возвращает success/failure — retry-worker сам решает
     * про markFailed/dead-letter.
     */
    @Transactional
    public EventProcessingResult assemble(UUID snapshotId) {
        TrassirSnapshot snapshot = snapshotRepository.findByIdForUpdate(snapshotId).orElse(null);
        if (snapshot == null) return EventProcessingResult.failure("Snapshot not found: " + snapshotId);
        if (snapshot.getCompletedAt() != null) return EventProcessingResult.success();
        return assembleInternal(snapshot);
    }

    private EventProcessingResult assembleInternal(TrassirSnapshot snapshot) {
        try {
            List<TrassirSnapshotPart> parts =
                    partRepository.findBySnapshotIdOrderByPartIndex(snapshot.getId());

            for (TrassirSnapshotPart part : parts) {
                JsonNode payload;
                try {
                    payload = objectMapper.readTree(part.getPayload());
                } catch (Exception e) {
                    return EventProcessingResult.failure(
                            "INVALID_TRASSIR_SNAPSHOT_PART: " + part.getPartIndex());
                }
                JsonNode objects = payload.get("objects");
                if (objects == null || !objects.isArray()) continue;

                for (JsonNode item : objects) {
                    String objectKey = text(item, "object_id");
                    if (objectKey == null) {
                        return EventProcessingResult.failure("Snapshot object_id is required");
                    }
                    String objectPayload = item.toString();
                    // FIX #28: атомарная вставка
                    snapshotObjectRepository.insertIfAbsent(
                            UUID.randomUUID(), snapshot.getId(), objectKey, objectPayload);

                    TrassirObject object = objectRepository
                            .findByServerIdAndObjectKey(snapshot.getServerId(), objectKey)
                            .orElseGet(() -> {
                                TrassirObject created = new TrassirObject();
                                created.setServerId(snapshot.getServerId());
                                created.setObjectKey(objectKey);
                                created.setFirstSeenAt(snapshot.getReceivedAt());
                                return created;
                            });
                    object.setObjectPayload(objectPayload);
                    object.setLastSeenAt(snapshot.getReceivedAt());
                    object.setDeletedAt(null);
                    objectRepository.save(object);
                }
            }

            Instant completedAt = Instant.now();
            int claimed = snapshotRepository.completeIfPending(snapshot.getId(), completedAt);
            if (claimed == 0) {
                // FIX #75: concurrent worker уже завершил — выходим без дублей.
                log.info("Snapshot {} already completed by concurrent worker", snapshot.getId());
                return EventProcessingResult.success();
            }
            // в assembleInternal после completeIfPending:
            TrassirServerState state = serverStateRepository.findById(snapshot.getServerId())
                    .orElseGet(() -> {
                        TrassirServerState created = new TrassirServerState();
                        created.setServerId(snapshot.getServerId());
                        return created;
                    });
            state.setLastSnapshotAt(completedAt);
            state.setUpdatedAt(completedAt);
            serverStateRepository.save(state);

            // FIX #30: stream_id = server_id (стабильный). Одна запись на сервер.
            TrassirSnapshotStream stream = streamRepository
                    .findByServerIdAndStreamId(snapshot.getServerId(), snapshot.getServerId())
                    .orElseGet(() -> {
                        TrassirSnapshotStream created = new TrassirSnapshotStream();
                        created.setServerId(snapshot.getServerId());
                        created.setStreamId(snapshot.getServerId());
                        return created;
                    });
            stream.setLastSnapshotId(snapshot.getId());
            stream.setLastPartIndex(snapshot.getTotalParts());
            stream.setUpdatedAt(completedAt);
            streamRepository.save(stream);

            // FIX #139: заполняем snapshot_seq порядковым номером
            // (ранее всегда 0). Оставляем 0 для обратной совместимости,
            // если колонка не критична — можно удалить в будущем.
            return EventProcessingResult.success();

        } catch (Exception e) {
            log.warn("Snapshot assembly failed: snapshotId={}", snapshot.getId(), e);
            return EventProcessingResult.failure(
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString() : null;
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isInt() ? value.asInt() : null;
    }
}