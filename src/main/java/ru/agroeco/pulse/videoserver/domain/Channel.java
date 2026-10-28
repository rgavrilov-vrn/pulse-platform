package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channels", schema = "videoserver")
public class Channel {

    @Id
    private UUID id;

    private UUID cameraId;

    private String externalId;

    private String name;

    private String status;

    private Instant createdAt;

    protected Channel() {
    }

    public Channel(UUID id, UUID cameraId, String externalId, String name, String status, Instant createdAt) {
        this.id = id;
        this.cameraId = cameraId;
        this.externalId = externalId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCameraId() {
        return cameraId;
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
