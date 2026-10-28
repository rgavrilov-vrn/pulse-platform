package ru.agroeco.pulse.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "outbox")
public class OutboxEvent {

    public enum Status {
        NEW,
        SENT,
        FAILED
    }

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(
        UUID id,
        String eventType,
        UUID entityId,
        String payload,
        Instant createdAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.entityId = entityId;
        this.payload = payload;
        this.status = Status.NEW.name();
        this.attempts = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isPending() {
        return Status.NEW.name().equals(status)
            && nextAttemptAt != null
            && !nextAttemptAt.isAfter(Instant.now());
    }

    public void markSent(Instant publishedAt) {
        this.status = Status.SENT.name();
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(int maxAttempts, Instant now, String error) {
        this.attempts = this.attempts + 1;
        this.lastError = error;

        if (this.attempts >= maxAttempts) {
            this.status = Status.FAILED.name();
            return;
        }

        long backoffSeconds = (long) Math.pow(2, this.attempts);
        this.nextAttemptAt = now.plusSeconds(backoffSeconds);
    }
}
