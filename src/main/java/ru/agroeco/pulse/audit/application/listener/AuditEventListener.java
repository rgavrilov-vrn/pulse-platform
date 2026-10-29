package ru.agroeco.pulse.audit.application.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    private static final String REQUEST_ID_MDC_KEY = "requestId";

    private final AuditEventRepository repository;
    private final ObjectMapper mapper;

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT,
            fallbackExecution = true   // сработать и вне транзакции (LOGIN_FAILED, Kafka-консьюмеры)
    )
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
                    .requestId(MDC.get(REQUEST_ID_MDC_KEY))
                    .sourceIp(currentSourceIp())
                    .userAgent(currentUserAgent())
                    .build());
        } catch (RuntimeException ex) {
            log.error("Failed to persist audit event action={} entity={}.{}/{}",
                    event.eventType().getValue(),
                    event.entitySchema(), event.entityTable(), event.entityId(),
                    ex);
            throw new IllegalStateException(
                    "Audit persistence failed for " + event.eventType().getValue(), ex);
        }
    }

    private String serialize(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s; // уже готовый JSON
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize audit payload", ex);
        }
    }

    private UUID currentActorId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
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