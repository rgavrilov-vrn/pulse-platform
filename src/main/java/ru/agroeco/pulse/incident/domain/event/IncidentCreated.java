package ru.agroeco.pulse.incident.domain.event;

import lombok.Builder;
import ru.agroeco.pulse.audit.domain.event.AuditAction;
import ru.agroeco.pulse.incident.domain.IncidentSeverity;
import ru.agroeco.pulse.incident.domain.IncidentStatus;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Доменное событие создания инцидента.
 *
 * Поля интеграции (sourceEventId, videoServerId, cameraId, channelId, objectId,
 * title) заполняются только для INTEGRATION-инцидентов. Для ручного пути — null.
 * Благодаря этому одно событие обслуживает и audit (см. newDataSnapshot),
 * и outbox/Kafka (полный payload для consumer'ов).
 */
@Builder
public record IncidentCreated(
        // ---- общие поля (audit + outbox) ----
        UUID incidentId,
        String number,
        String source,
        IncidentStatus status,
        IncidentSeverity severity,
        Instant eventTime,
        UUID actorId,
        Instant occurredAt,

        // ---- интеграционный контекст (nullable для ручного пути) ----
        UUID sourceEventId,       // eventId из VideoEventNormalized
        String sourceEventType,   // "VideoEventNormalized" / null
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,
        String title
) implements DomainEvent {

    @Override public AuditAction eventType() { return AuditAction.INCIDENT_CREATED; }
    @Override public UUID entityId() { return incidentId; }
    @Override public String entitySchema() { return "incident"; }
    @Override public String entityTable() { return "incidents"; }
    @Override public Instant occurredAt() { return occurredAt; }
    @Override public UUID actorId() { return actorId; }

    /**
     * Kafka-контракт для интеграционных событий исторически — PascalCase.
     * application.yml.app.outbox.event-topics уже содержит ключ "IncidentCreated".
     * Для audit это неважно: AuditEventListener использует eventType().getValue().
     */
    @Override
    public String outboxEventType() {
        return "IncidentCreated";
    }

    /**
     * Audit пишет минимум полей — «что изменилось в сущности».
     * Полный интеграционный контекст в audit_log не нужен (и не должен туда течь).
     */
    @Override
    public Object newDataSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("number", number);
        data.put("source", source);
        data.put("status", status == null ? null : status.name());
        data.put("severity", severity == null ? null : severity.name());
        return data;
    }

    // outboxPayload() не переопределяем: по умолчанию = this (record),
    // сериализуется целиком, включая nullable поля интеграции.
}