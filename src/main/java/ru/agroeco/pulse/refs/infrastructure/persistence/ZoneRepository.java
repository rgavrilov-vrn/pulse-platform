package ru.agroeco.pulse.refs.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.agroeco.pulse.refs.domain.Zone;

import java.util.UUID;

public interface ZoneRepository
        extends JpaRepository<Zone, UUID>,
        JpaSpecificationExecutor<Zone> {
}