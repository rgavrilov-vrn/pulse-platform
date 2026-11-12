package ru.agroeco.pulse.trassir.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class TrassirEventRouter {

    private final TrassirMetrics metrics;
    private final Map<String, TrassirEventUseCase> handlers;

    /**
     * FIX: убран @RequiredArgsConstructor — он генерировал второй конструктор
     * на (TrassirMetrics, Map), Spring не мог выбрать между ним и этим.
     * Плюс @RequiredArgsConstructor попытался бы заинжектить
     * Map<String, TrassirEventUseCase> из всех бинов TrassirEventUseCase,
     * но с ключами по имени бина (applyAgentHeartbeatUseCase и т.д.),
     * а не по event_type (agent.heartbeat).
     */
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
     * FIX #81: неизвестный тип не пропускаем молча (раньше success()),
     * а возвращаем failure — событие уйдёт в FAILED/retry,
     * по метрике trassir_events_unknown_total будут алерты.
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