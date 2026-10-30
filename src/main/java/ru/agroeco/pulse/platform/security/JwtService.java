package ru.agroeco.pulse.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:pulse-platform}") String issuer,
            @Value("${app.security.jwt.audience:pulse-api}") String audience
    ) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret must be at least 32 bytes (256 bits) for HS256; got "
                            + secretBytes.length + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationSeconds = Duration.ofMinutes(expirationMinutes).toSeconds();
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .claims(Map.of("roles",
                        user.getAuthorities().stream().map(Object::toString).toList()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            Claims c = parse(token);
            return user.getUsername().equals(c.getSubject())
                    && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}