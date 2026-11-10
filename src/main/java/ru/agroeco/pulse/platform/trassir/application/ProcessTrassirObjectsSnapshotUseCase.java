package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshot;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotObject;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotPart;
import ru.agroeco.pulse.trassir.domain.TrassirSnapshotStream;
import ru.agroeco.pulse.trassir.domain.TrassirObject;
import ru.agroeco.pulse.trassir.repository.TrassirObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotPartRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotStreamRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessTrassirObjectsSnapshotUseCase implements TrassirEventUseCase {

    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotPartRepository partRepository;
    private final TrassirSnapshotStreamRepository streamRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final TrassirObjectRepository objectRepository;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();

        String snapshotId = text(data, "snapshot_id");
        Integer totalParts = intValue(data, "parts_total");
        Integer partIndex = intValue(data, "part_index");

        if (snapshotId == null || totalParts == null || partIndex == null) {
            return EventProcessingResult.failure(
                    "Snapshot data.snapshot_id, parts_total and part_index are required"
            );
        }

        UUID externalSnapshotId;
        try {
            externalSnapshotId = UUID.fromString(snapshotId);
        } catch (IllegalArgumentException e) {
            return EventProcessingResult.failure("Snapshot data.snapshot_id must be a valid UUID");
        }

        TrassirSnapshot snapshot = snapshotRepository
                .findByServerIdAndExternalSnapshotId(context.serverId(), externalSnapshotId)
                .orElseGet(() -> {
                    TrassirSnapshot created = new TrassirSnapshot();
                    created.setExternalSnapshotId(externalSnapshotId);
                    created.setServerId(context.serverId());
                    created.setStreamId(snapshotId);
                    created.setSnapshotSeq(0L);
                    created.setTotalParts(totalParts);
                    created.setReceivedAt(context.event().observedAt());
                    created.setStatus("PENDING");
                    created.setRetryCount(0);
                    return snapshotRepository.save(created);
                });

        if (snapshot.getCompletedAt() != null) {
            return EventProcessingResult.success();
        }

        if (!partRepository.existsBySnapshotIdAndPartIndex(snapshot.getId(), partIndex)) {
            TrassirSnapshotPart part = new TrassirSnapshotPart();
            part.setSnapshotId(snapshot.getId());
            part.setPartIndex(partIndex);
            part.setPayload(toJson(context.event().data()));
            part.setReceivedAt(context.event().observedAt());
            partRepository.save(part);
        }

        if (partRepository.countBySnapshotId(snapshot.getId()) < totalParts) {
            return EventProcessingResult.success();
        }

        try {
            List<TrassirSnapshotPart> parts = partRepository.findBySnapshotIdOrderByPartIndex(snapshot.getId());
            for (TrassirSnapshotPart storedPart : parts) {
                JsonNode payload;
                try {
                    payload = objectMapper.readTree(storedPart.getPayload());
                } catch (Exception e) {
                    return failure(snapshot, "INVALID_TRASSIR_SNAPSHOT_PART");
                }

                JsonNode objects = payload.get("objects");
                if (objects == null || !objects.isArray()) {
                    continue;
                }

                for (JsonNode item : objects) {
                    String objectKey = text(item, "object_id");
                    if (objectKey == null) {
                        return failure(snapshot, "Snapshot object_id is required");
                    }

                    String objectPayload = item.toString();

                    TrassirSnapshotObject snapshotObject = new TrassirSnapshotObject();
                    snapshotObject.setSnapshotId(snapshot.getId());
                    snapshotObject.setObjectKey(objectKey);
                    snapshotObject.setObjectPayload(objectPayload);
                    snapshotObjectRepository.save(snapshotObject);

                    TrassirObject object = objectRepository
                            .findByServerIdAndObjectKey(context.serverId(), objectKey)
                            .orElseGet(() -> {
                                TrassirObject created = new TrassirObject();
                                created.setServerId(context.serverId());
                                created.setObjectKey(objectKey);
                                created.setFirstSeenAt(context.event().observedAt());
                                return created;
                            });
                    object.setObjectPayload(objectPayload);
                    object.setLastSeenAt(context.event().observedAt());
                    object.setDeletedAt(null);
                    objectRepository.save(object);
                }
            }

            snapshotRepository.completeIfPending(snapshot.getId(), Instant.now());

            TrassirSnapshotStream stream = streamRepository
                    .findByServerIdAndStreamId(context.serverId(), snapshotId)
                    .orElseGet(() -> {
                        TrassirSnapshotStream created = new TrassirSnapshotStream();
                        created.setServerId(context.serverId());
                        created.setStreamId(snapshotId);
                        return created;
                    });
            stream.setLastSnapshotId(snapshot.getId());
            stream.setLastPartIndex(partIndex);
            stream.setUpdatedAt(Instant.now());
            streamRepository.save(stream);

            return EventProcessingResult.success();
        } catch (Exception e) {
            return failure(snapshot, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private EventProcessingResult failure(TrassirSnapshot snapshot, String error) {
        snapshotRepository.markFailed(snapshot.getId(), error, Instant.now());
        return EventProcessingResult.failure(error);
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank()
                ? value.asString()
                : null;
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isInt() ? value.asInt() : null;
    }
}
