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
@Table(name = "ingest_raw", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirIngestRaw {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private UUID batchId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "producer_name", nullable = false)
    private String producerName;

    @Column(name = "producer_version")
    private String producerVersion;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_body", nullable = false)
    private String rawBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
