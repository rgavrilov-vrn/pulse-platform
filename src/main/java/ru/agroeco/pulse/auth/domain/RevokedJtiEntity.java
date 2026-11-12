package ru.agroeco.pulse.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "revoked_jti", schema = "auth")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevokedJtiEntity {

    @Id
    @Column(name = "jti", length = 64, nullable = false)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "reason", length = 100)
    private String reason;
}