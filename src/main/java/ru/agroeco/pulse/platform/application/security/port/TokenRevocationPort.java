package ru.agroeco.pulse.platform.application.security.port;

import java.time.Instant;

/**
 * FIX #48 + Modulith: порт для проверки/отзыва JWT. Реализация — в auth.application.
 * Platform не должен зависеть от auth напрямую.
 */
public interface TokenRevocationPort {

    boolean isRevoked(String jti);

    void revoke(String jti, Instant expiresAt, String reason);
}