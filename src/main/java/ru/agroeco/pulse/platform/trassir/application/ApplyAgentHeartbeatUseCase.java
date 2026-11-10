package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.trassir.repository.TrassirAgentHeartbeatRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirServerStateRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApplyAgentHeartbeatUseCase implements TrassirEventUseCase {

    private final TrassirAgentHeartbeatRepository heartbeatRepository;
    private final TrassirServerStateRepository serverStateRepository;
    private final ObjectMapper objectMapper;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data;
        try {
            data = objectMapper.readTree(context.event().data());
        } catch (Exception e) {
            return EventProcessingResult.failure("INVALID_TRASSIR_EVENT_DATA");
        }

        String component = text(data, "component");
        String status = text(data, "status");

        if (component == null || status == null) {
            return EventProcessingResult.failure(
                    "Heartbeat data.component and data.status must be nonblank strings"
            );
        }

        TrassirServerState state = serverStateRepository
                .findById(context.serverId())
                .orElseGet(() -> {
                    TrassirServerState created = new TrassirServerState();
                    created.setServerId(context.serverId());
                    return created;
                });
        state.setLastHeartbeatAt(context.event().observedAt());
        state.setUpdatedAt(Instant.now());
        serverStateRepository.save(state);

        TrassirAgentHeartbeat heartbeat = new TrassirAgentHeartbeat();
        heartbeat.setServerId(context.serverId());
        heartbeat.setObservedAt(context.event().observedAt());
        heartbeat.setComponent(component);
        heartbeat.setStatus(status);
        heartbeat.setDetails(context.event().data());
        heartbeatRepository.save(heartbeat);

        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText()
                : null;
    }
}
