package ru.agroeco.pulse.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // FIX #47: case-insensitive Bearer (RFC 6750).
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LEN = BEARER_PREFIX.length();

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX_LEN)) {
            String token = header.substring(BEARER_PREFIX_LEN);
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    // FIX #20: явная проверка isEnabled / isAccountNonLocked —
                    // не полагаемся только на фильтр в AppUserDetailsService.
                    if (user.isEnabled()
                            && user.isAccountNonLocked()
                            && user.isAccountNonExpired()
                            && user.isCredentialsNonExpired()
                            && jwtService.isValid(token, user)) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                log.debug("Invalid JWT: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}