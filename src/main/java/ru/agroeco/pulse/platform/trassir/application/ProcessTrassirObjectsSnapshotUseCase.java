package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.trassir.repository.TrassirObjectRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirSnapshotObjectRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirSnapshotPartRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirSnapshotRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirSnapshotStreamRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

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
        JsonNode data;
        try {
            data = objectMapper.readTree(context.event().data());
        } catch (Exception e) {
            return EventProcessingResult.failure("INVALID_TRASSIR_EVENT_DATA");
        }

        String streamId = text(data, "stream_id");
        Long snapshotSeq = longValue(data, "snapshot_seq");
        Integer totalParts = intValue(data, "total_parts");
        Integer partIndex = intValue(data, "part_index");

        if (streamId == null || snapshotSeq == null || totalParts == null || partIndex == null) {
            return EventProcessingResult.failure(
                    "Snapshot data.stream_id, snapshot_seq, total_parts and part_index are required"
            );
        }

        TrassirSnapshot snapshot = snapshotRepository
                .findByServerIdAndStreamIdAndSnapshotSeq(
                        context.serverId(), streamId, snapshotSeq
                )
                .orElseGet(() -> {
                    TrassirSnapshot created = new TrassirSnapshot();
                    created.setServerId(context.serverId());
                    created.setStreamId(streamId);
                    created.setSnapshotSeq(snapshotSeq);
                    created.setTotalParts(totalParts);
                    created.setReceivedAt(context.event().observedAt());
                    return created;
                });
        snapshot = snapshotRepository.save(snapshot);

        TrassirSnapshotPart part = new TrassirSnapshotPart();
        part.setSnapshotId(snapshot.getId());
        part.setPartIndex(partIndex);
        part.setPayload(context.event().data());
        part.setReceivedAt(context.event().observedAt());
        partRepository.save(part);

        if (partRepository.countBySnapshotId(snapshot.getId()) < totalParts) {
            return EventProcessingResult.success();
        }

        snapshot.setCompletedAt(Instant.now());
        snapshotRepository.save(snapshot);

        List<TrassirSnapshotPart> parts = partRepository.findBySnapshotIdOrderByPartIndex(snapshot.getId());
        for (TrassirSnapshotPart storedPart : parts) {
            JsonNode payload;
            try {
                payload = objectMapper.readTree(storedPart.getPayload());
            } catch (Exception e) {
                return EventProcessingResult.failure("INVALID_TRASSIR_SNAPSHOT_PART");
            }

            JsonNode objects = payload.get("objects");
            if (objects == null || !objects.isArray()) {
                continue;
            }

            for (JsonNode item : objects) {
                String objectKey = text(item, "object_key");
                if (objectKey == null) {
                    return EventProcessingResult.failure("Snapshot object_key is required");
                }

                String objectPayload = item.get("payload") != null
                        ? item.get("payload").toString()
                        : "{}";

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

        TrassirSnapshotStream stream = streamRepository
                .findByServerIdAndStreamId(context.serverId(), streamId)
                .orElseGet(() -> {
                    TrassirSnapshotStream created = new TrassirSnapshotStream();
                    created.setServerId(context.serverId());
                    created.setStreamId(streamId);
                    return created;
                });
        stream.setLastSnapshotId(snapshot.getId());
        stream.setLastPartIndex(partIndex);
        stream.setUpdatedAt(Instant.now());
        streamRepository.save(stream);

        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText()
                : null;
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.canConvertToLong() ? value.asLong() : null;
    }

    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isInt() ? value.asInt() : null;
    }
}
