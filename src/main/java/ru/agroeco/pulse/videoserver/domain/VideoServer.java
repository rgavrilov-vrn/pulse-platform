package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_servers", schema = "videoserver")
public class VideoServer {

    @Id
    private UUID id;

    private String name;

    private String baseUrl;

    private String status;

    private Instant createdAt;

    protected VideoServer() {
    }

    public VideoServer(UUID id, String name, String baseUrl, String status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
