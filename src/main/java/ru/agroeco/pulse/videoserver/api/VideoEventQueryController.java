package ru.agroeco.pulse.videoserver.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.videoserver.domain.NormalizedVideoEvent;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.NormalizedVideoEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/videoserver/events")
@RequiredArgsConstructor
public class VideoEventQueryController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "occurredAt", "normalizedAt", "id", "severity", "eventType");
    private static final String DEFAULT_SORT_FIELD = "occurredAt";

    private final NormalizedVideoEventRepository repository;

    @GetMapping
    @Operation(
            summary = "List video events",
            parameters = {
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sorting. Allowed fields: occurredAt, normalizedAt, id",
                            example = "occurredAt,DESC"),
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Page number (0-based)", example = "0"),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Page size (1..100)", example = "20")
            }
    )
    public Page<VideoEventNormalized> list(
            @RequestParam(required = false) UUID videoServerId,
            @RequestParam(required = false) UUID cameraId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(
                    size = 20,
                    sort = DEFAULT_SORT_FIELD,
                    direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Pageable safePageable = sanitize(pageable);

        try {
            validateRange(from, to);
            return fetchPage(videoServerId, cameraId, from, to, safePageable)
                    .map(this::toDto);

        } catch (BusinessRuleException ex) {
            log.warn("Invalid query for video events: {}", ex.getMessage());
            throw ex;
        } catch (DataAccessException ex) {
            log.error("DB error: serverId={}, cameraId={}, from={}, to={}, pageable={}",
                    videoServerId, cameraId, from, to, safePageable, ex);
            throw ex;
        }
    }

    private Pageable sanitize(Pageable pageable) {
        List<Sort.Order> valid = pageable.getSort().stream()
                .filter(o -> ALLOWED_SORT_FIELDS.contains(o.getProperty()))
                .toList();

        Sort sort = valid.isEmpty()
                ? Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)
                : Sort.by(valid);

        if (valid.size() < pageable.getSort().stream().count()) {
            log.debug("Dropped unsupported sort properties, remaining: {}", sort);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Page<NormalizedVideoEvent> fetchPage(
            UUID videoServerId, UUID cameraId, Instant from, Instant to, Pageable pageable) {
        if (cameraId != null) {
            return repository.findByCameraId(cameraId, pageable);
        }
        if (videoServerId != null && from != null && to != null) {
            return repository.findByVideoServerIdAndOccurredAtBetween(
                    videoServerId, from, to, pageable);
        }
        if (videoServerId != null) {
            return repository.findByVideoServerId(videoServerId, pageable);
        }
        return repository.findAll(pageable);
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_DATE_RANGE: 'from' must be <= 'to' (from=" + from + ", to=" + to + ")");
        }
    }

    private VideoEventNormalized toDto(NormalizedVideoEvent n) {
        return new VideoEventNormalized(
                n.getId(), n.getRawEventId(), n.getVideoServerId(),
                n.getCameraId(), n.getChannelId(), n.getObjectId(),
                n.getOccurredAt(), n.getSource(),
                n.getEventType(), n.getSeverity(), n.getPayloadVersion());
    }
}