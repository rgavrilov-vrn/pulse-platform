package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSystemEvent;

import java.util.UUID;

public interface TrassirSystemEventRepository extends JpaRepository<TrassirSystemEvent, UUID> {
}
