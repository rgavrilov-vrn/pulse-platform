package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "cameras",
        schema = "videoserver"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Camera {

    @Id private UUID id;

    @Column(name = "video_server_id", nullable = false)
    private UUID videoServerId;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "external_id", length = 255)
    private String externalId;                     // = TRASSIR object_key

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

    @Column(name = "last_seen_at")   private Instant lastSeenAt;
    @Column(name = "deleted_at")     private Instant deletedAt;
    @Column(name = "deactivated_by", length = 32) private String deactivatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Camera(UUID id, UUID videoServerId, String source, String externalId,
                  String code, String name, String description,
                  CameraStatus status, boolean isActive) {
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

    // --- domain mutators для reconcile ---

    public void applySnapshot(String name, String code, String description, Instant seenAt) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.lastSeenAt = seenAt;
        this.deletedAt = null;
        this.deactivatedBy = null;
        this.isActive = true;
    }

    public void softDeleteByAgent(Instant at) {
        this.deletedAt = at;
        this.deactivatedBy = "agent_snapshot";
        this.isActive = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}