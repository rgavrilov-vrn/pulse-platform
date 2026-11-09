package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;

import java.util.UUID;

public interface TrassirEventRepository extends JpaRepository<TrassirEvent, UUID> {
}
