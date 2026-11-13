package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "normalized_video_events",
        schema = "videoserver",
        indexes = {
                @Index(name = "idx_norm_events_camera_occurred",  columnList = "camera_id, occurred_at"),
                @Index(name = "idx_norm_events_server_occurred",  columnList = "video_server_id, occurred_at"),
                @Index(name = "idx_norm_events_channel_occurred", columnList = "channel_id, occurred_at"),
                @Index(name = "ix_norm_events_source_event",      columnList = "source_event_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NormalizedVideoEvent {

    @Id private UUID id;

    @Column(name = "video_server_id", nullable = false) private UUID videoServerId;
    @Column(name = "camera_id")                          private UUID cameraId;
    @Column(name = "channel_id")                         private UUID channelId;
    @Column(name = "object_id")                          private UUID objectId;

    @Column(name = "source_event_id", nullable = false, updatable = false)
    private UUID sourceEventId;                    // = trassir.events.event_id

    @Column(name = "external_event_id", nullable = false, updatable = false, length = 255)
    private String externalEventId;

    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false, length = 64)          private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 128) private VideoEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32) private EventSeverity severity;

    @Column(name = "payload_version", nullable = false) private int payloadVersion;
    @Column(name = "normalized_at", nullable = false)   private Instant normalizedAt;
    @Column(name = "ingested_at", nullable = false)     private Instant ingestedAt;

    public NormalizedVideoEvent(UUID id,
                                UUID videoServerId, UUID cameraId, UUID channelId, UUID objectId,
                                UUID sourceEventId, String externalEventId,
                                Instant occurredAt, String source,
                                VideoEventType eventType, EventSeverity severity,
                                int payloadVersion,
                                Instant normalizedAt, Instant ingestedAt) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.cameraId = cameraId;
        this.channelId = channelId;
        this.objectId = objectId;
        this.sourceEventId = sourceEventId;
        this.externalEventId = externalEventId;
        this.occurredAt = occurredAt;
        this.source = source;
        this.eventType = eventType;
        this.severity = severity;
        this.payloadVersion = payloadVersion;
        this.normalizedAt = normalizedAt;
        this.ingestedAt = ingestedAt;
    }
}