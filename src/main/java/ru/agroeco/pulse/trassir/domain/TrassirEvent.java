package ru.agroeco.pulse.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "raw_id", nullable = false, updatable = false)
    private UUID rawId;

    @Column(name = "server_id", nullable = false, updatable = false)
    private UUID serverId;

    @Column(name = "event_index", nullable = false, updatable = false)
    private Integer eventIndex;

    @Column(name = "event_id", nullable = false, unique = true, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false, updatable = false)
    private String eventVersion;

    @Column(name = "occurred_at", updatable = false)
    private Instant occurredAt;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private Instant observedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_event", nullable = false, updatable = false)
    private String rawEvent;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "processing_error")
    private String processingError;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}
