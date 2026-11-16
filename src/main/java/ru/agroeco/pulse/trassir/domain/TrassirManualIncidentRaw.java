package ru.agroeco.pulse.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_incidents_raw", schema = "trassir")
@Getter @Setter @NoArgsConstructor
public class TrassirManualIncidentRaw {
    @Id private UUID id;
    @Column(name = "event_id", nullable = false, unique = true) private UUID eventId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "observed_at", nullable = false) private Instant observedAt;
    @Column(name = "object_id", nullable = false) private String objectId;
    @Column(name = "object_name") private String objectName;
    @Column(name = "object_class") private String objectClass;
    @Column(columnDefinition = "text") private String comment;
    private String reason;
    @Column(name = "video_timestamp") private String videoTimestamp;
    @Column(name = "image_file", columnDefinition = "text") private String imageFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}