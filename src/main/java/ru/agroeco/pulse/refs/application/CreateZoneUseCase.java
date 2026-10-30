package ru.agroeco.pulse.refs.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.refs.api.CreateZoneRequest;
import ru.agroeco.pulse.refs.api.ZoneResponse;
import ru.agroeco.pulse.refs.domain.Zone;
import ru.agroeco.pulse.refs.infrastructure.persistence.FacilityRepository;
import ru.agroeco.pulse.refs.infrastructure.persistence.ZoneRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateZoneUseCase {

    private final ZoneRepository zoneRepository;
    private final FacilityRepository facilityRepository;

    @Transactional
    public ZoneResponse handle(CreateZoneRequest request) {
        if (!facilityRepository.existsById(request.objectId())) {
            throw new BusinessRuleException("FACILITY_NOT_FOUND: id=" + request.objectId());
        }
        if (request.parentId() != null) {
            Zone parent = zoneRepository.findById(request.parentId())
                    .orElseThrow(() -> new BusinessRuleException(
                            "PARENT_ZONE_NOT_FOUND: id=" + request.parentId()));
            if (!parent.getObjectId().equals(request.objectId())) {
                throw new BusinessRuleException(
                        "PARENT_ZONE_FOREIGN_FACILITY: parent.objectId=" + parent.getObjectId()
                                + ", request.objectId=" + request.objectId());
            }
        }

        Zone entity = new Zone(
                UUID.randomUUID(),
                request.objectId(),
                request.parentId(),
                request.code().trim(),
                request.name().trim(),
                request.description(),
                request.isActive() == null || request.isActive()
        );
        try {
            return toResponse(zoneRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate zone code: objectId={}, code={}",
                    request.objectId(), request.code());
            throw new BusinessRuleException(
                    "ZONE_CODE_ALREADY_EXISTS: objectId=" + request.objectId()
                            + ", code=" + request.code());
        }
    }

    private ZoneResponse toResponse(Zone z) {
        return new ZoneResponse(
                z.getId(), z.getObjectId(), z.getParentId(),
                z.getCode(), z.getName(), z.getDescription(),
                z.isActive(), z.getCreatedAt(), z.getUpdatedAt());
    }
}