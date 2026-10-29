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
        name = "cameras",
        schema = "videoserver",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cameras_source_external",
                columnNames = {"source", "external_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Camera {

    @Id
    private UUID id;

    @Column(name = "video_server_id")
    private UUID videoServerId;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CameraStatus status;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Camera(UUID id, UUID videoServerId,
                  String source, String externalId, String code, String name,
                  String description, CameraStatus status, boolean isActive) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.source = source;
        this.externalId = externalId;
        this.code = code;
        this.name = name;
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