package ru.agroeco.pulse.platform.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "server_state", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirServerState {

    @Id
    @Column(name = "server_id")
    private String serverId;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_snapshot_at")
    private Instant lastSnapshotAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
