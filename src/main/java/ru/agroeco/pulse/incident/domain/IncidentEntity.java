package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "incidents", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentEntity {

    @Id
    private UUID id;

    /** Генерируется триггером set_incident_number — Hibernate вставляет NULL,
     *  после INSERT читает значение из БД. */
    @Generated(event = EventType.INSERT)
    @Column(name = "number", insertable = false, updatable = false, length = 30)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false)
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

    @Column(name = "resolution_comment", columnDefinition = "text")
    private String resolutionComment;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(columnDefinition = "text")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 32)
    private IncidentKind kind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (metadata == null) metadata = Map.of();
    }
}