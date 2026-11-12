package ru.agroeco.pulse.platform.api.support;

import org.slf4j.MDC;

/**
 * FIX #95: единственный источник константы «correlationId».
 * Раньше дублировалась в GlobalExceptionHandler, RequestLoggingFilter,
 * AuditEventListener, SecurityConfig — при рефакторинге ломалась бы одна
 * из копий, и аудит/логи теряли бы корреляцию.
 */
public final class CorrelationIds {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER  = "X-Correlation-Id";

    private CorrelationIds() {}

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void set(String id) {
        if (id == null || id.isBlank()) return;
        MDC.put(MDC_KEY, id);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}