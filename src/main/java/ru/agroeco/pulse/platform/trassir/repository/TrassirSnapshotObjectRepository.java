package ru.agroeco.pulse.platform.trassir.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.platform.trassir.domain.TrassirSnapshotObject;

import java.util.UUID;

public interface TrassirSnapshotObjectRepository extends JpaRepository<TrassirSnapshotObject, UUID> {
}
