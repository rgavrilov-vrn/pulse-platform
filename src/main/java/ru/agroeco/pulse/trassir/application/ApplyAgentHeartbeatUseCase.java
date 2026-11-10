package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.domain.TrassirAgentHeartbeat;
import ru.agroeco.pulse.trassir.domain.TrassirServerState;
import ru.agroeco.pulse.trassir.repository.TrassirAgentHeartbeatRepository;
import ru.agroeco.pulse.trassir.repository.TrassirServerStateRepository;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApplyAgentHeartbeatUseCase implements TrassirEventUseCase {
    private final TrassirAgentHeartbeatRepository heartbeatRepository;
    private final TrassirServerStateRepository serverStateRepository;

    @Override
    public EventProcessingResult handle(TrassirEventContext context) {
        JsonNode data = context.event().data();
        String component = text(data, "component");
        if (component == null) {
            return EventProcessingResult.failure("Heartbeat data.component must be a nonblank string");
        }
        String status = text(data, "status");
        if (status == null) {
            status = "UNKNOWN";
        }
        TrassirServerState state = serverStateRepository.findById(context.serverId()).orElseGet(() -> {
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
        heartbeat.setDetails(data.toString());
        heartbeatRepository.save(heartbeat);
        return EventProcessingResult.success();
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode value = node.get(field);
        return value != null && value.isString() && !value.asString().isBlank() ? value.asString() : null;
    }
}
