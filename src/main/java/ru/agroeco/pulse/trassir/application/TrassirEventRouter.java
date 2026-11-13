package ru.agroeco.pulse.trassir.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TrassirEventRouter {

    private final TrassirMetrics metrics;
    private final Map<String, TrassirEventUseCase> handlers;

    public TrassirEventRouter(TrassirMetrics metrics,
                              ApplyAgentHeartbeatUseCase heartbeat,
                              ProcessTrassirObjectsSnapshotUseCase snapshot,
                              ApplyTrassirSystemEventUseCase systemEvent,
                              ApplyTrassirVideoEventUseCase videoEvent) {
        this.metrics = metrics;
        this.handlers = Map.of(
                "agent.heartbeat",        heartbeat,
                "agent.objects_snapshot", snapshot,
                "trassir.system_event",   systemEvent,
                "trassir.video_event",    videoEvent
        );
    }

    public EventProcessingResult route(TrassirEventContext context) {
        TrassirEventUseCase handler = handlers.get(context.event().eventType());
        if (handler == null) {
            log.warn("Unknown TRASSIR event type: {} (eventId={}, serverId={})",
                    context.event().eventType(),
                    context.event().eventId(),
                    context.serverId());
            metrics.incrementEventUnknown(context.event().eventType());
            return EventProcessingResult.failure(
                    "UNKNOWN_EVENT_TYPE: " + context.event().eventType());
        }
        return handler.handle(context);
    }
}