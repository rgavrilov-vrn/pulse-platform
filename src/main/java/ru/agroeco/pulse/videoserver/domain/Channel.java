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
@Table(name = "channels", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    private UUID id;

    private UUID cameraId;

    private String externalId;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChannelStatus status;

    private Instant createdAt;

    public Channel(UUID id, UUID cameraId, String externalId, String name,
                   ChannelStatus status, Instant createdAt) {
        this.id = id;
        this.cameraId = cameraId;
        this.externalId = externalId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }
}