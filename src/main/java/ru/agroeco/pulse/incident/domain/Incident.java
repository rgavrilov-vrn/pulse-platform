package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents", schema = "incident")
public class Incident {

    @Id
    private UUID id;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "source_event_type", nullable = false, length = 100)
    private String sourceEventType;

    @Column(name = "video_server_id")
    private UUID videoServerId;

    @Column(name = "camera_id")
    private UUID cameraId;

    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "object_id")
    private UUID objectId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column
    private String resolution;

    protected Incident() {
    }

    public Incident(
        UUID id,
        UUID sourceEventId,
        String sourceEventType,
        UUID videoServerId,
        UUID cameraId,
        UUID channelId,
        UUID objectId,
        String severity,
        String title,
        String description,
        Instant occurredAt,
        Instant createdAt
    ) {
        this.id = id;
        this.sourceEventId = sourceEventId;
        this.sourceEventType = sourceEventType;
        this.videoServerId = videoServerId;
        this.cameraId = cameraId;
        this.channelId = channelId;
        this.objectId = objectId;
        this.status = IncidentStatus.NEW.name();
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public String getSourceEventType() {
        return sourceEventType;
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

    public String getStatus() {
        return status;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getResolution() {
        return resolution;
    }
}
