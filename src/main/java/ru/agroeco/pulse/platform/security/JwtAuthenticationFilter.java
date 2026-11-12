package ru.agroeco.pulse.platform.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LEN = BEARER_PREFIX.length();

    private final JwtService jwtService;
    // FIX #44: loadUserById вместо loadUserByUsername (sub=userId).
    private final UserDetailsByIdService userDetailsByIdService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX_LEN)) {
            String token = header.substring(BEARER_PREFIX_LEN);
            try {
                // FIX #45: один parse. Claims переиспользуется для sub/jti/exp.
                Claims claims = jwtService.parse(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UUID userId = parseSubject(claims);
                    if (userId != null) {
                        UserDetails user = userDetailsByIdService.loadUserById(userId);
                        if (user.isEnabled()
                                && user.isAccountNonLocked()
                                && user.isAccountNonExpired()
                                && user.isCredentialsNonExpired()
                                && jwtService.isValid(claims, user)) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    user, null, user.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Invalid JWT: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    /** sub=userId после FIX #44. Fallback на username (legacy-токены) — 401. */
    private static UUID parseSubject(Claims claims) {
        String sub = claims.getSubject();
        if (sub == null) return null;
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            // Legacy-токен (sub=username) — несовместим. Требуем перелогин.
            return null;
        }
    }
}