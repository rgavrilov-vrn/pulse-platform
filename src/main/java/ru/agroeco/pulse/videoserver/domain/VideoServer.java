package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "video_servers",
        schema = "videoserver",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_video_servers_source_external",
                columnNames = {"source", "external_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoServer {

    @Id
    private UUID id;

    @Column(name = "object_id")
    private UUID objectId;

    @Column(name = "zone_id")
    private UUID zoneId;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 255)
    private String name;

    private String host;

    @Column(name = "base_url", length = 512)
    private String baseUrl;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private VideoServerStatus status;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public VideoServer(UUID id, UUID objectId, UUID zoneId, String source, String externalId, String name,
                       String host, String baseUrl, String description,
                       VideoServerStatus status, boolean isActive) {
        this.id = id;
        this.objectId = objectId;
        this.zoneId = zoneId;
        this.source = source;
        this.externalId = externalId;
        this.name = name;
        this.host = host;
        this.baseUrl = baseUrl;
        this.description = description;
        this.status = status;
        this.isActive = isActive;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}