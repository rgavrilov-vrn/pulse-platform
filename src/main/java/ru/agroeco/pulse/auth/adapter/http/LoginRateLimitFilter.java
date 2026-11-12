package ru.agroeco.pulse.auth.adapter.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.agroeco.pulse.auth.application.LoginRateLimiterProperties;
import ru.agroeco.pulse.platform.application.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FIX #42: rate-limit на /api/v1/auth/token — защита от brute-force.
 *
 * <p>Только по IP. Ограничение per-username требует чтения body (ломает
 * downstream), а распределённые атаки (много IP → один username) — вне
 * scope приложения, решается на WAF/edge.</p>
 *
 * <p>Caffeine — окно с TTL. На distributed-инстансах счётчики независимы;
 * общая защита при необходимости — через Redis.</p>
 */
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final LoginRateLimiterProperties props;
    private final ObjectMapper mapper;
    private final Cache<String, AtomicInteger> counters;

    public LoginRateLimitFilter(LoginRateLimiterProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.counters = Caffeine.newBuilder()
                .expireAfterWrite(props.getWindow())
                .maximumSize(100_000)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/token".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = clientKey(request);
        AtomicInteger counter = counters.get(key, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() > props.getMaxAttempts()) {
            log.warn("Login rate-limit exceeded: key={}", key);
            writeError(response, mapper, 429, "TOO_MANY_REQUESTS",
                    "Too many login attempts, try again later");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        // X-Forwarded-For при прокси (первый адрес — клиент).
        // ВНИМАНИЕ: header spoofable при отсутствии доверенного прокси.
        // В production за reverse-proxy проверять, что запрос пришёл от прокси.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static void writeError(HttpServletResponse res, ObjectMapper mapper,
                                   int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new ErrorResponse(
                Instant.now(), status, code, message,
                List.of(), null, MDC.get(CORRELATION_ID_MDC_KEY));
        mapper.writeValue(res.getOutputStream(), body);
    }
}