package ru.agroeco.pulse.trassir.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log", schema = "trassir")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditLog {

    @Id @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor", nullable = false)        private String actor;
    @Column(name = "action", nullable = false)       private String action;
    @Column(name = "target", nullable = false)       private String target;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "correlation_id", length = 100) private String correlationId;
    @Column(name = "request_id", length = 100)     private String requestId;
    @Column(name = "status", nullable = false, length = 32) private String status;
    @Column(name = "error_message", columnDefinition = "text") private String errorMessage;
    @Column(name = "affected_count") private Integer affectedCount;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "SUCCESS";
    }
}