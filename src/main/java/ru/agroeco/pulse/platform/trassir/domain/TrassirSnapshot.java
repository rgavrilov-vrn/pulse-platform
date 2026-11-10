package ru.agroeco.pulse.platform.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "snapshots", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "external_snapshot_id", nullable = false, unique = true)
    private UUID externalSnapshotId;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "stream_id", nullable = false)
    private String streamId;

    @Column(name = "snapshot_seq", nullable = false)
    private Long snapshotSeq;

    @Column(name = "total_parts", nullable = false)
    private Integer totalParts;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
}
