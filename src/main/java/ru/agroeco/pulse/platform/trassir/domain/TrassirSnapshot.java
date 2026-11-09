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
}
