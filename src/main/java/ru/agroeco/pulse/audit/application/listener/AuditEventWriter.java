package ru.agroeco.pulse.audit.application.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventEntity;
import ru.agroeco.pulse.audit.adapter.persistence.AuditEventRepository;

@Component
@RequiredArgsConstructor
public class AuditEventWriter {

    private final AuditEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeIsolated(AuditEventEntity entity) {
        repository.saveAndFlush(entity);
    }

    /**
     * FIX #74: saveAndFlush вместо save. Раньше INSERT выполнялся на commit
     * транзакции — вне try/catch из AuditEventListener.on(). Ошибка (unique
     * violation, size limit) классифицировалась как DATA_INTEGRITY_VIOLATION
     * вместо AUDIT_FAILURE, маскируя причину.
     *
     * С saveAndFlush INSERT выполняется внутри вызова → try/catch ловит,
     * транзакция откатывается, клиент видит осмысленный код.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeInTransaction(AuditEventEntity entity) {
        repository.saveAndFlush(entity);
    }
}