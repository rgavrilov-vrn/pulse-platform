package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirServerState;

public interface TrassirServerStateRepository extends JpaRepository<TrassirServerState, String> {
}
