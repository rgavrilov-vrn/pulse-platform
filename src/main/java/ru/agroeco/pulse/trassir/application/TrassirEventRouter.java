package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirEventRouter {

    private final TrassirMetrics metrics;
    private final Map<String, TrassirEventUseCase> handlers;

    public TrassirEventRouter(TrassirMetrics metrics,
                              ApplyAgentHeartbeatUseCase heartbeat,
                              ProcessTrassirObjectsSnapshotUseCase snapshot,
                              ApplyTrassirSystemEventUseCase systemEvent) {
        this.metrics = metrics;
        this.handlers = Map.of(
                "agent.heartbeat",        heartbeat,
                "agent.objects_snapshot", snapshot,
                "trassir.system_event",   systemEvent);
    }

    /**
     * FIX #81: раньше неизвестный event_type возвращал success() —
     * событие помечалось PROCESSED, но фактически отбрасывалось
     * (тихая потеря). Теперь — failure: событие уйдёт в FAILED
     * и retry, а по метрике trassir_events_unknown_total будут алерты.
     */
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