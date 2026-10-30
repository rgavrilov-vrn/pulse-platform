package ru.agroeco.pulse.refs.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;
import ru.agroeco.pulse.refs.api.CreateEmployeeRequest;
import ru.agroeco.pulse.refs.api.EmployeeResponse;
import ru.agroeco.pulse.refs.domain.Employee;
import ru.agroeco.pulse.refs.domain.event.EmployeeCreated;
import ru.agroeco.pulse.refs.infrastructure.persistence.DepartmentRepository;
import ru.agroeco.pulse.refs.infrastructure.persistence.EmployeeRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EventDispatcher events;

    @Transactional
    public EmployeeResponse handle(CreateEmployeeRequest request) {
        if (request.departmentId() != null
                && !departmentRepository.existsById(request.departmentId())) {
            throw new NotFoundException("Department not found: id=" + request.departmentId());
        }

        String personnelNo = request.personnelNo() != null
                && !request.personnelNo().isBlank()
                ? request.personnelNo().trim() : null;

        Employee entity = new Employee(
                UUID.randomUUID(),
                personnelNo,
                request.fullName().trim(),
                request.departmentId(),
                request.isActive() == null || request.isActive()
        );

        Employee saved;
        try {
            saved = employeeRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate employee personnelNo: {}", personnelNo);
            throw new BusinessRuleException(
                    "EMPLOYEE_PERSONNEL_NO_ALREADY_EXISTS: personnelNo=" + personnelNo);
        }

        events.dispatch(EmployeeCreated.builder()
                .employeeId(saved.getId())
                .personnelNo(saved.getPersonnelNo())
                .fullName(saved.getFullName())
                .departmentId(saved.getDepartmentId())
                .actorId(null)
                .occurredAt(Instant.now())
                .build());

        return toResponse(saved);
    }

    private EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getId(), e.getPersonnelNo(), e.getFullName(),
                e.getDepartmentId(), e.isActive(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}