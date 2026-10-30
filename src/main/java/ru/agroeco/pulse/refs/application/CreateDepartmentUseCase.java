package ru.agroeco.pulse.refs.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.refs.api.CreateDepartmentRequest;
import ru.agroeco.pulse.refs.api.DepartmentResponse;
import ru.agroeco.pulse.refs.domain.Department;
import ru.agroeco.pulse.refs.infrastructure.persistence.DepartmentRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDepartmentUseCase {

    private final DepartmentRepository repository;

    @Transactional
    public DepartmentResponse handle(CreateDepartmentRequest request) {
        Department entity = new Department(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                request.isActive() == null || request.isActive()
        );
        try {
            return toResponse(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate department code: {}", request.code());
            throw new BusinessRuleException(
                    "DEPARTMENT_CODE_ALREADY_EXISTS: code=" + request.code());
        }
    }

    private DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(
                d.getId(), d.getCode(), d.getName(),
                d.isActive(), d.getCreatedAt(), d.getUpdatedAt());
    }
}