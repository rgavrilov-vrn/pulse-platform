package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "raw_video_events", schema = "videoserver")
public class RawVideoEvent {

    @Id
    private UUID id;

    private UUID videoServerId;

    private UUID cameraId;

    private UUID channelId;

    private Instant occurredAt;

    private String source;

    private String rawPayload;

    private Instant ingestedAt;

    protected RawVideoEvent() {
    }

    public RawVideoEvent(
        UUID id,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        Instant occurredAt,
        String source,
        String rawPayload,
        Instant ingestedAt
    ) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.cameraId = cameraId;
        this.channelId = channelId;
        this.occurredAt = occurredAt;
        this.source = source;
        this.rawPayload = rawPayload;
        this.ingestedAt = ingestedAt;
    }

    public UUID getId() {
        return id;
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSource() {
        return source;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
