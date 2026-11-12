package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.auth.adapter.persistence.RevokedJtiRepository;
import ru.agroeco.pulse.auth.domain.RevokedJtiEntity;
import ru.agroeco.pulse.platform.security.TokenRevocationPort;

import java.time.Instant;

/**
 * FIX #48: отзыв JWT по jti.
 *
 * <p>Реализует {@link TokenRevocationPort} из platform.security — это
 * позволяет JwtService зависеть от интерфейса в platform, а не от auth
 * (Modulith: platform не может зависеть от auth).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService implements TokenRevocationPort {

    private final RevokedJtiRepository repository;

    /** REQUIRES_NEW — revocation должна переживать откат родительской транзакции. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(String jti, Instant expiresAt, String reason) {
        if (jti == null || jti.isBlank()) return;
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            log.debug("Token {} already expired; skip revocation", jti);
            return;
        }
        if (repository.existsByJti(jti)) {
            log.debug("Token {} already revoked", jti);
            return;
        }
        repository.save(RevokedJtiEntity.builder()
                .jti(jti)
                .expiresAt(expiresAt)
                .revokedAt(Instant.now())
                .reason(reason)
                .build());
        log.info("Token revoked: jti={}, expiresAt={}, reason={}", jti, expiresAt, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) return false;
        return repository.existsByJti(jti);
    }

    /** Cleanup: раз в час. Удаляет записи с истёкшим exp. */
    @Scheduled(fixedDelayString = "${app.security.jwt.revocation-cleanup-ms:3600000}")
    @Transactional
    public void cleanup() {
        int deleted = repository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Revoked JWT cleanup: {} expired entries removed", deleted);
        }
    }
}