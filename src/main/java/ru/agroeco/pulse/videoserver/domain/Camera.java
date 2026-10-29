package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cameras", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Camera {

    @Id
    private UUID id;

    private UUID videoServerId;

    private String externalId;

    private String name;

    @Enumerated(EnumType.STRING)
    private CameraStatus status;

    private Instant createdAt;

    public Camera(UUID id, UUID videoServerId, String externalId, String name,
                  CameraStatus status, Instant createdAt) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.externalId = externalId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }
}