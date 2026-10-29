package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "normalized_video_events",
        schema = "videoserver",
        indexes = {
                @Index(name = "idx_norm_events_camera_occurred",
                        columnList = "camera_id, occurred_at"),
                @Index(name = "idx_norm_events_server_occurred",
                        columnList = "video_server_id, occurred_at"),
                @Index(name = "idx_norm_events_raw", columnList = "raw_event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NormalizedVideoEvent {

    @Id
    private UUID id;

    private UUID rawEventId;

    private UUID videoServerId;

    private UUID cameraId;

    private UUID channelId;

    private UUID objectId;

    private Instant occurredAt;

    private String source;

    @Enumerated(EnumType.STRING)
    private VideoEventType eventType;

    @Enumerated(EnumType.STRING)
    private EventSeverity severity;

    private int payloadVersion;

    private Instant normalizedAt;

    public NormalizedVideoEvent(
            UUID id,
            UUID rawEventId,
            UUID videoServerId,
            UUID cameraId,
            UUID channelId,
            UUID objectId,
            Instant occurredAt,
            String source,
            VideoEventType eventType,
            EventSeverity severity,
            int payloadVersion,
            Instant normalizedAt
    ) {
        this.id = id;
        this.rawEventId = rawEventId;
        this.videoServerId = videoServerId;
        this.cameraId = cameraId;
        this.channelId = channelId;
        this.objectId = objectId;
        this.occurredAt = occurredAt;
        this.source = source;
        this.eventType = eventType;
        this.severity = severity;
        this.payloadVersion = payloadVersion;
        this.normalizedAt = normalizedAt;
    }
}