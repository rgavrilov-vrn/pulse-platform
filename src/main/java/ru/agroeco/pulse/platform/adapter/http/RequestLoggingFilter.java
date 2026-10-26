package ru.agroeco.pulse.platform.adapter.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID, correlationId);
        response.setHeader("X-Correlation-Id", correlationId);
        long startedAt = System.currentTimeMillis();
        try {
            log.info("HTTP request: {} {}", request.getMethod(), request.getRequestURI());
            chain.doFilter(request, response);
        } finally {
            log.info("HTTP response: {} {} status={} durationMs={}", request.getMethod(), request.getRequestURI(), response.getStatus(), System.currentTimeMillis() - startedAt);
            MDC.remove(CORRELATION_ID);
        }
    }
}
