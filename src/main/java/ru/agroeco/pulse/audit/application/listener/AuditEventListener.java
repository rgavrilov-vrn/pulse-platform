package ru.agroeco.pulse.audit.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventEntity;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventRepository;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.security.AppPrincipal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    /** Синхронизировано с RequestLoggingFilter.CORRELATION_ID. */
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final String AUTH_SCHEMA = "auth";

    private final AuditEventRepository repository;
    private final ObjectMapper mapper;

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = true)
    public void on(DomainEvent event) {
        try {
            repository.save(AuditEventEntity.builder()
                    .action(event.eventType().getValue())
                    .actorId(event.actorId() != null ? event.actorId() : currentActorId())
                    .entitySchema(event.entitySchema())
                    .entityTable(event.entityTable())
                    .entityId(event.entityId())
                    .oldData(serialize(event.oldDataSnapshot()))
                    .newData(serialize(event.newDataSnapshot()))
                    .occurredAt(event.occurredAt() != null ? event.occurredAt() : Instant.now())
                    .requestId(MDC.get(CORRELATION_ID_MDC_KEY))
                    .sourceIp(currentSourceIp())
                    .userAgent(currentUserAgent())
                    .build());
        } catch (RuntimeException ex) {
            if (AUTH_SCHEMA.equals(event.entitySchema())) {
                // Auth-события: fail-open. Ошибка аудита не должна превращать
                // 401 (неверный пароль) в 500 (сервер упал) — иначе DoS через
                // флуд логинами.
                log.error("Auth audit failed for action={} user={}",
                        event.eventType().getValue(), event.entityId(), ex);
            } else {
                // Бизнес-события: fail-closed. Аудит — часть контракта.
                log.error("Audit persistence failed for action={} entity={}.{}/{}",
                        event.eventType().getValue(),
                        event.entitySchema(), event.entityTable(), event.entityId(), ex);
                throw new IllegalStateException(
                        "Audit persistence failed for " + event.eventType().getValue(), ex);
            }
        }
    }

    private String serialize(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return mapper.writeValueAsString(value);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Cannot serialize audit payload", ex);
        }
    }

    private UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getPrincipal() instanceof AppPrincipal p ? p.id() : null;
    }

    private InetAddress currentSourceIp() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return null;
        try {
            return InetAddress.getByName(attrs.getRequest().getRemoteAddr());
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private String currentUserAgent() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest().getHeader("User-Agent");
    }
}