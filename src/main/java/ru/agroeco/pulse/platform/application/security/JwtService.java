package ru.agroeco.pulse.platform.application.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import ru.agroeco.pulse.platform.application.security.port.TokenRevocationPort;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT access-token service.
 *
 * <p>Изменения относительно исходной версии:</p>
 * <ul>
 *   <li><b>#44</b> — {@code sub} = userId (UUID), {@code username} — отдельным claim.
 *       Фильтр загружает пользователя по id; смена login'а не инвалидирует токены.</li>
 *   <li><b>#45</b> — публичный {@link #parse(String)} возвращает {@link Claims};
 *       фильтр переиспользует один результат. Раньше {@code extractUsername} +
 *       {@code isValid} парсили токен дважды (HMAC + base64 decode на каждый запрос).</li>
 *   <li><b>#48</b> — проверка отзыва по {@code jti} через {@link TokenRevocationPort}.
 *       Раньше единственный способ «убить» токен — дождаться {@code exp} (60 минут).</li>
 * </ul>
 *
 * <p>Модульная граница: зависит только от platform-интерфейса
 * {@link TokenRevocationPort}, реализация — в {@code auth.application}.
 * Platform не импортирует auth → {@code SpringModulith.verify()} чист.</p>
 */
@Slf4j
@Service
public class JwtService {

    /** Минимальная длина HMAC-SHA256 ключа — 256 бит (32 байта). */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES    = "roles";

    private final SecretKey key;
    private final long expirationSeconds;
    private final String issuer;
    private final String audience;
    private final TokenRevocationPort revocation;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:pulse-platform}") String issuer,
            @Value("${app.security.jwt.audience:pulse-api}") String audience,
            TokenRevocationPort revocation
    ) {
        // Fail-fast на старте: пустой или короткий секрет → приложение не поднимется,
        // а не «тихо выдаст невалидные токены».
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256; got " + secretBytes.length + " bytes");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException(
                    "app.security.jwt.expiration-minutes must be positive; got " + expirationMinutes);
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationSeconds = Duration.ofMinutes(expirationMinutes).toSeconds();
        this.issuer = issuer;
        this.audience = audience;
        this.revocation = revocation;
    }

    // ---------------------------------------------------------------- issue

    /**
     * Генерирует подписанный JWT.
     *
     * <p>FIX #44: {@code sub} = {@code AppPrincipal.id()} (UUID). Для прочих
     * {@link UserDetails} (например, тестовых) — fallback на username, но
     * {@code JwtAuthenticationFilter} такие токены отклоняет (sub не парсится
     * как UUID) → эффективно «только AppPrincipal».</p>
     */
    public String generateToken(UserDetails user) {
        Instant now = Instant.now();

        String subject = (user instanceof AppPrincipal p && p.id() != null)
                ? p.id().toString()
                : user.getUsername();

        // LinkedHashMap вместо Map.of — чтобы при расширении состава claims
        // не падать на null-значениях (Map.of NPE).
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put(CLAIM_ROLES, user.getAuthorities().stream()
                .map(Object::toString)
                .toList());

        return Jwts.builder()
                .subject(subject)
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())          // jti — для revocation
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)                             // HS256 по умолчанию для ключа ≥256 бит
                .compact();
    }

    // ----------------------------------------------------------------- parse

    /**
     * FIX #45: единственный parse на запрос.
     *
     * <p>Проверяет подпись, {@code iss}, {@code aud}, {@code exp}.
     * Бросает {@link JwtException} (или подкласс) на любой невалидный токен —
     * вызывающий код должен ловить.</p>
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * FIX #45: работает над уже разобранным {@link Claims} — не парсит повторно.
     * FIX #48: проверяет отзыв по {@code jti}.
     *
     * <p>Умышленно НЕ проверяет {@code sub == userId}: это уже сделал вызывающий
     * (он загрузил {@code UserDetails} по {@code sub}). Здесь — только
     * «согласованность username и статус токена».</p>
     */
    public boolean isValid(Claims claims, UserDetails user) {
        if (claims == null || user == null) return false;
        try {
            // 1. exp
            Date exp = claims.getExpiration();
            if (exp == null || !exp.after(new Date())) {
                return false;
            }

            // 2. revocation по jti
            String jti = claims.getId();
            if (jti != null && revocation.isRevoked(jti)) {
                log.debug("JWT rejected: revoked jti={}", jti);
                return false;
            }

            // 3. username — мягкая проверка. Если claim есть — должен совпадать.
            //    Если нет (legacy/тестовый токен) — полагаемся на sub, который
            //    проверил вызывающий при loadUserById.
            String usernameInToken = claims.get(CLAIM_USERNAME, String.class);
            return usernameInToken == null || usernameInToken.equals(user.getUsername());
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------- extracts

    /**
     * FIX #48: {@code jti} для logout / revoke.
     * Парсит токен — вызывать только там, где это приемлемо (logout, admin).
     */
    public String extractJti(String token) {
        return parse(token).getId();
    }

    /** {@code exp} для записи в {@code auth.revoked_jti}. */
    public Instant extractExpiration(String token) {
        Date exp = parse(token).getExpiration();
        return exp == null ? null : exp.toInstant();
    }

    /** {@code sub} — userId (или legacy username для старых токенов). */
    public String extractSubject(String token) {
        return parse(token).getSubject();
    }

    /** {@code username} claim. */
    public String extractUsername(String token) {
        return parse(token).get(CLAIM_USERNAME, String.class);
    }

    // ---------------------------------------------------------------- helpers

    /** TTL в секундах — для {@code TokenResponse.expiresInSeconds}. */
    public long expirationSeconds() {
        return expirationSeconds;
    }
}