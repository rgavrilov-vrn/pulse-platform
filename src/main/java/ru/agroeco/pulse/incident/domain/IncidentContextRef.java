package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "context_refs", schema = "incident")
@IdClass(IncidentContextRefId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentContextRef {

    @Id
    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Id
    @Column(name = "domain", nullable = false, length = 64)
    private String domain;

    @Id
    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Id
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "entity_name_snapshot", length = 255)
    private String entityNameSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public IncidentContextRef(UUID incidentId, String domain, String entityType,
                              UUID entityId, boolean isPrimary, String entityNameSnapshot) {
        this.incidentId = incidentId;
        this.domain = domain;
        this.entityType = entityType;
        this.entityId = entityId;
        this.isPrimary = isPrimary;
        this.entityNameSnapshot = entityNameSnapshot;
    }
}