package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.SourceEntity;

public interface SourceRepository extends JpaRepository<SourceEntity, String> {
}