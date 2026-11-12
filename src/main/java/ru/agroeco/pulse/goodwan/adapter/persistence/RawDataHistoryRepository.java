package ru.agroeco.pulse.goodwan.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.goodwan.domain.RawDataHistoryEntity;

import java.util.Optional;

public interface RawDataHistoryRepository
        extends JpaRepository<RawDataHistoryEntity, Long> {

    boolean existsByExternalDeviceIdAndSourceRecordId(
        String externalDeviceId,
        Long sourceRecordId
    );

    Optional<RawDataHistoryEntity> findByExternalDeviceIdAndSourceRecordId(
        String externalDeviceId,
        Long sourceRecordId
    );
}
