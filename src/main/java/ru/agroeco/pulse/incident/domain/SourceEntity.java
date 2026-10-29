package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sources", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceEntity {

    @Id
    @Column(name = "code", length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}