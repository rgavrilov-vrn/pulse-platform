package ru.agroeco.pulse.refs.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.refs.api.CreateDepartmentRequest;
import ru.agroeco.pulse.refs.api.DepartmentResponse;
import ru.agroeco.pulse.refs.domain.Department;
import ru.agroeco.pulse.refs.domain.event.DepartmentCreated;
import ru.agroeco.pulse.refs.infrastructure.persistence.DepartmentRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDepartmentUseCase {

    private final DepartmentRepository repository;
    private final EventDispatcher events;

    @Transactional
    public DepartmentResponse handle(CreateDepartmentRequest request) {
        Department entity = new Department(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                request.isActive() == null || request.isActive()
        );

        Department saved;
        try {
            saved = repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate department code: {}", request.code());
            throw new BusinessRuleException(
                    "DEPARTMENT_CODE_ALREADY_EXISTS: code=" + request.code());
        }

        events.dispatch(DepartmentCreated.builder()
                .departmentId(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .actorId(null)
                .occurredAt(Instant.now())
                .build());

        return toResponse(saved);
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
                d.getId(), d.getCode(), d.getName(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}