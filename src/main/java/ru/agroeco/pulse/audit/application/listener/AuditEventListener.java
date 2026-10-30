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
import ru.agroeco.pulse.audit.domain.exception.AuditException;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.security.AppPrincipal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    /** Синхронизировано с RequestLoggingFilter.CORRELATION_ID. */
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final String AUTH_SCHEMA = "auth";

    /**
     * Только эти действия fail-open. Дискриминируем по action, а не по схеме:
     * USER_ROLE_CHANGED тоже живёт в auth, но должен быть fail-closed.
     */
    private static final Set<String> FAIL_OPEN_ACTIONS = Set.of(
            "USER_AUTHENTICATED",
            "USER_AUTHENTICATION_FAILED",
            "USER_LOGGED_OUT"
    );

    private final AuditEventWriter writer;   // ← вместо repository
    private final ObjectMapper mapper;

    /**
     * Бизнес-события: fail-closed.
     * Аудит — часть контракта транзакции, ошибка записи откатывает всё.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = true)
    public void on(DomainEvent event) {
        if (isFailOpen(event)) return;
        try {
            writer.writeInTransaction(buildEntity(event));
        } catch (RuntimeException ex) {
            log.error("Audit persistence failed for action={} entity={}.{}/{}",
                    event.eventType().getValue(),
                    event.entitySchema(), event.entityTable(), event.entityId(), ex);
            throw new AuditException(
                    "Audit persistence failed for " + event.eventType().getValue(), ex);
        }
    }

    /**
     * Auth-события: fail-open, отдельная транзакция.
     *
     * AFTER_COMPLETION, а не AFTER_COMMIT: провал логина может завершаться
     * rollback'ом (BadCredentialsException после публикации события), и мы
     * всё равно обязаны записать попытку. REQUIRES_NEW изолирует аудит
     * от судьбы бизнес-транзакции — 401 не превращается в 500.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMPLETION,
            fallbackExecution = true)
    public void onAuth(DomainEvent event) {
        if (!isFailOpen(event)) return;
        try {
            writer.writeIsolated(buildEntity(event));   // ← proxy целиком внутри try
        } catch (RuntimeException ex) {
            log.error("Auth audit failed for action={} entityId={}",
                    event.eventType().getValue(), event.entityId(), ex);
            // fail-open: не пробрасываем
        }
    }

    private boolean isFailOpen(DomainEvent event) {
        return AUTH_SCHEMA.equals(event.entitySchema())
                && FAIL_OPEN_ACTIONS.contains(event.eventType().getValue());
    }

    private AuditEventEntity buildEntity(DomainEvent event) {
        return AuditEventEntity.builder()
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
                .build();
    }

    private String serialize(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return mapper.writeValueAsString(value);
        } catch (RuntimeException ex) {
            throw new AuditException("Cannot serialize audit payload", ex);
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
        String remoteAddr = attrs.getRequest().getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) return null;
        try {
            // getByName не делает DNS для IP-литералов — JDK распознаёт их сразу.
            return InetAddress.getByName(remoteAddr);
        } catch (UnknownHostException ex) {
            // Не роняем аудит из-за мусорного remoteAddr (mock-запросы, edge-кейсы прокси).
            return null;
        }
    }

    private String currentUserAgent() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest().getHeader("User-Agent");
    }
}