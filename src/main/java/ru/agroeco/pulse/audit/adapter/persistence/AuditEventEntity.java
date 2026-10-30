package ru.agroeco.pulse.audit.adapter.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import ru.agroeco.pulse.audit.domain.AuditEventId;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log", schema = "audit")
@IdClass(AuditEventId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEventEntity {

    /**
     * Composite PK (id, occurred_at) — обязательное условие для
     * партиционированной таблицы в Postgres (partition key должен быть в PK).
     *
     * UUID генерится на клиенте: BIGSERIAL/IDENTITY несовместим с composite PK.
     */
    @Id
    @UuidGenerator
    private UUID id;

    @Id
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_schema", nullable = false, length = 100)
    private String entitySchema;

    @Column(name = "entity_table", nullable = false, length = 100)
    private String entityTable;

    /** Nullable: auth-события (USER_AUTHENTICATION_FAILED) не имеют сущности. */
    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "jsonb")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb")
    private String newData;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "source_ip")
    private InetAddress sourceIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @PrePersist
    void prePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}