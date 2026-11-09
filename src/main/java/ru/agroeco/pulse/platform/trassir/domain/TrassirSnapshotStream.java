package ru.agroeco.pulse.platform.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "snapshot_streams", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirSnapshotStream {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "stream_id", nullable = false)
    private String streamId;

    @Column(name = "last_snapshot_id")
    private UUID lastSnapshotId;

    @Column(name = "last_part_index")
    private Integer lastPartIndex;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
