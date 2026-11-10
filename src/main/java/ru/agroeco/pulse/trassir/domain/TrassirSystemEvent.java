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
@Table(name = "system_events", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirSystemEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private String eventVersion;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(nullable = false)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_event", nullable = false)
    private String rawEvent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
