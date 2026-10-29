package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "raw_video_events",
        schema = "videoserver",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raw_video_events_server_external",
                columnNames = {"video_server_id", "external_event_id"}
        ),
        indexes = {
                @Index(name = "idx_raw_video_events_camera", columnList = "camera_id"),
                @Index(name = "idx_raw_video_events_occurred", columnList = "occurred_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawVideoEvent {

    @Id
    private UUID id;

    private UUID videoServerId;

    private UUID cameraId;

    private UUID channelId;

    @Column(name = "external_event_id", nullable = false, length = 200)
    private String externalEventId;

    private Instant occurredAt;

    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private String rawPayload;

    private Instant ingestedAt;

    public RawVideoEvent(
            UUID id,
            UUID videoServerId,
            UUID cameraId,
            UUID channelId,
            String externalEventId,
            Instant occurredAt,
            String source,
            String rawPayload,
            Instant ingestedAt
    ) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.cameraId = cameraId;
        this.channelId = channelId;
        this.externalEventId = externalEventId;
        this.occurredAt = occurredAt;
        this.source = source;
        this.rawPayload = rawPayload;
        this.ingestedAt = ingestedAt;
    }
}