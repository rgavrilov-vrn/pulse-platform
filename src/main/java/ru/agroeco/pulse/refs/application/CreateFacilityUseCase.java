package ru.agroeco.pulse.refs.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.refs.api.CreateFacilityRequest;
import ru.agroeco.pulse.refs.api.FacilityResponse;
import ru.agroeco.pulse.refs.domain.Facility;
import ru.agroeco.pulse.refs.domain.event.FacilityCreated;
import ru.agroeco.pulse.refs.infrastructure.persistence.FacilityRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateFacilityUseCase {

    private final FacilityRepository repository;
    private final EventDispatcher events;

    @Transactional
    public FacilityResponse handle(CreateFacilityRequest request) {
        Facility entity = new Facility(
                UUID.randomUUID(),
                request.code().trim(),
                request.name().trim(),
                request.description(),
                request.address(),
                request.isActive() == null || request.isActive()
        );

        Facility saved;
        try {
            saved = repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate facility code: {}", request.code());
            throw new BusinessRuleException(
                    "FACILITY_CODE_ALREADY_EXISTS: code=" + request.code());
        }

        events.dispatch(FacilityCreated.builder()
                .facilityId(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .actorId(null)          // AuditEventListener подставит из SecurityContext
                .occurredAt(Instant.now())
                .build());

        return toResponse(saved);
    }

    private FacilityResponse toResponse(Facility f) {
        return new FacilityResponse(
                f.getId(), f.getCode(), f.getName(),
                f.getDescription(), f.getAddress(),
                f.isActive(), f.getCreatedAt(), f.getUpdatedAt());
    }
}