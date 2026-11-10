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
@Table(name = "objects", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirObject {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private String serverId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "object_payload", nullable = false)
    private String objectPayload;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
