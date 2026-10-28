package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "normalized_video_events", schema = "videoserver")
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

    private String eventType;

    private String severity;

    private Integer payloadVersion;

    private Instant normalizedAt;

    protected NormalizedVideoEvent() {
    }

    public NormalizedVideoEvent(
        UUID id,
        UUID rawEventId,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,
        Instant occurredAt,
        String source,
        String eventType,
        String severity,
        Integer payloadVersion,
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

    public UUID getId() {
        return id;
    }

    public UUID getRawEventId() {
        return rawEventId;
    }

    public UUID getVideoServerId() {
        return videoServerId;
    }

    public UUID getCameraId() {
        return cameraId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSource() {
        return source;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public Integer getPayloadVersion() {
        return payloadVersion;
    }

    public Instant getNormalizedAt() {
        return normalizedAt;
    }
}
