package ru.agroeco.pulse.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "snapshot_objects", schema = "trassir")
@Getter
@Setter
@NoArgsConstructor
public class TrassirSnapshotObject {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "object_payload", nullable = false)
    private String objectPayload;
}
