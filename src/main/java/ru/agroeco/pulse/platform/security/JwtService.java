package ru.agroeco.pulse.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(@Value("${app.security.jwt.secret}") String secret,
                      @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getUsername())
                .claims(Map.of("roles", user.getAuthorities().stream().map(Object::toString).toList()))
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key).compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token, UserDetails user) {
        try {
            Claims c = parse(token);
            return user.getUsername().equals(c.getSubject()) && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
