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
@Table(name = "video_servers", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoServer {

    @Id
    private UUID id;

    private String name;

    private String baseUrl;

    @Enumerated(EnumType.STRING)
    private VideoServerStatus status;

    private Instant createdAt;

    public VideoServer(UUID id, String name, String baseUrl,
                       VideoServerStatus status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.status = status;
        this.createdAt = createdAt;
    }
}