package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "incidents", schema = "incident")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {

    @Id
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(name = "number", nullable = false, insertable = false, updatable = false, length = 30)
    private String number;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "source", nullable = false, length = 64)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "creation_mode", nullable = false, length = 32)
    private IncidentCreationMode creationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private IncidentSeverity severity;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "resolution_comment")
    private String resolutionComment;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "comment")
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";

    public Incident(
            UUID id,
            String source,
            IncidentCreationMode creationMode,
            IncidentStatus status,
            IncidentSeverity severity,
            Instant eventTime,
            Instant receivedAt,
            Instant createdAt,
            UUID createdBy
    ) {
        this.id = id;
        this.source = source;
        this.creationMode = creationMode;
        this.status = status;
        this.severity = severity;
        this.eventTime = eventTime;
        this.receivedAt = receivedAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.createdBy = createdBy;
    }
}