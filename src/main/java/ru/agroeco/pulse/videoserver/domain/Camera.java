package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cameras", schema = "videoserver")
public class Camera {

    @Id
    private UUID id;

    private UUID videoServerId;

    private String externalId;

    private String name;

    private String status;

    private Instant createdAt;

    protected Camera() {
    }

    public Camera(UUID id, UUID videoServerId, String externalId, String name, String status, Instant createdAt) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.externalId = externalId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVideoServerId() {
        return videoServerId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
