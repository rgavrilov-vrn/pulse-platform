package ru.agroeco.pulse.refs.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.refs.domain.Facility;

import java.util.UUID;

public interface FacilityRepository
        extends JpaRepository<Facility, UUID>,
        JpaSpecificationExecutor<Facility> {
}