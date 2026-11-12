package ru.agroeco.pulse.audit.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import tools.jackson.databind.ObjectMapper;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventEntity;
import ru.agroeco.pulse.audit.domain.exception.AuditException;
import ru.agroeco.pulse.platform.domain.event.DomainEvent;
import ru.agroeco.pulse.platform.domain.event.DomainEventType;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private static final String AUTH_SCHEMA = "auth";

    /**
     * FIX #132: Set<DomainEventType> вместо Set<String>. Компилятор подскажет,
     * если добавят новый auth-тип и забудут про fail-open.
     */
    private static final Set<DomainEventType> FAIL_OPEN_EVENT_TYPES = Set.of(
            DomainEventType.USER_AUTHENTICATED,
            DomainEventType.USER_AUTHENTICATION_FAILED,
            DomainEventType.USER_LOGGED_OUT
    );

    private final AuditEventWriter writer;
    private final ObjectMapper mapper;

    /**
     * Бизнес-события: fail-closed.
     *
     * FIX #73: fallbackExecution = false. Раньше при dispatch business-события
     * вне транзакции on() вызывался, writeInTransaction (MANDATORY) бросал
     * IllegalTransactionStateException, on() заворачивал в AuditException —
     * и publishEvent() падал с 500 у вызывающего. Теперь: dispatch вне
     * транзакции = аудит молча пропускается (тихая потеря лучше 500), а
     * контракт «все business-события — из @Transactional» фиксируется в коде.
     *
     * Если нужен fail-closed на «событие без транзакции» — использовать
     * ApplicationEventPublisher из @Transactional-метода или проверять
     * TransactionSynchronizationManager.isActualTransactionActive() в сервисах.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = false)
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
     * fallbackExecution = true — auth-событие может публиковаться вне
     * транзакции (например, из job или при ошибке аутентификации),
     * и аудит всё равно должен сработать.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMPLETION,
            fallbackExecution = true)
    public void onAuth(DomainEvent event) {
        if (!isFailOpen(event)) return;
        try {
            writer.writeIsolated(buildEntity(event));
        } catch (RuntimeException ex) {
            log.error("Auth audit failed for action={} entityId={}",
                    event.eventType().getValue(), event.entityId(), ex);
        }
    }

    private boolean isFailOpen(DomainEvent event) {
        return AUTH_SCHEMA.equals(event.entitySchema())
                && FAIL_OPEN_EVENT_TYPES.contains(event.eventType());
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
                .requestId(CorrelationIds.current())
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
            return InetAddress.getByName(remoteAddr);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private String currentUserAgent() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest().getHeader("User-Agent");
    }
}