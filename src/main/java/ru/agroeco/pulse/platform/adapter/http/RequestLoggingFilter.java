package ru.agroeco.pulse.platform.adapter.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String correlationId = request.getHeader(CorrelationIds.HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        CorrelationIds.set(correlationId);
        response.setHeader(CorrelationIds.HEADER, correlationId);

        long startedAt = System.currentTimeMillis();
        try {
            // FIX #93: DEBUG — на 1000 rps INFO даёт 2000 записей/сек,
            // синхронный appender становится узким местом.
            if (log.isDebugEnabled()) {
                log.debug("HTTP request: {} {}", request.getMethod(), request.getRequestURI());
            }
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            // INFO — только для медленных (≥1s) или ошибочных (5xx).
            // Остальное — DEBUG (уже снаружи if, но лог-уровень сам отфильтрует).
            int status = response.getStatus();
            if (status >= 500 || durationMs >= 1000) {
                log.info("HTTP slow/error: {} {} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs);
            } else if (log.isDebugEnabled()) {
                log.debug("HTTP response: {} {} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), status, durationMs);
            }
            CorrelationIds.clear();
        }
    }
}