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

    /**
     * REQUIRES_NEW: аудит auth-событий не разделяет судьбу бизнес-транзакции.
     * saveAndFlush: гарантирует, что INSERT выполнится внутри вызова
     * (а не в commit-хуке), чтобы вызывающий мог поймать ошибку.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeIsolated(AuditEventEntity entity) {
        repository.saveAndFlush(entity);
    }

    /**
     * Пишет в текущей транзакции (fail-closed: ошибка откатит бизнес-операцию).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void writeInTransaction(AuditEventEntity entity) {
        repository.save(entity);
    }
}