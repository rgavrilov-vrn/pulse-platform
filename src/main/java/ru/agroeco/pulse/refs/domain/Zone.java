package ru.agroeco.pulse.refs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "zones",
        schema = "ref",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_zones_object_code",
                columnNames = {"object_id", "code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone {

    @Id
    private UUID id;

    @Column(name = "object_id", nullable = false)
    private UUID objectId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Zone(UUID id, UUID objectId, UUID parentId,
                String code, String name, String description, boolean isActive) {
        this.id = id;
        this.objectId = objectId;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
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