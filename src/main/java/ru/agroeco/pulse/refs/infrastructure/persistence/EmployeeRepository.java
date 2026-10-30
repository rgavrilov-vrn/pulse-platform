package ru.agroeco.pulse.refs.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.refs.domain.Employee;

import java.util.UUID;

public interface EmployeeRepository
        extends JpaRepository<Employee, UUID>,
        JpaSpecificationExecutor<Employee> {
}