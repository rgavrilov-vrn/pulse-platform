package ru.agroeco.pulse.goodwan.raw;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
