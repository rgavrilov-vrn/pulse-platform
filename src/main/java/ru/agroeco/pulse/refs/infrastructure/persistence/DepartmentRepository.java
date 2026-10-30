package ru.agroeco.pulse.refs.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.refs.domain.Department;

import java.util.UUID;

public interface DepartmentRepository
        extends JpaRepository<Department, UUID>,
        JpaSpecificationExecutor<Department> {
}