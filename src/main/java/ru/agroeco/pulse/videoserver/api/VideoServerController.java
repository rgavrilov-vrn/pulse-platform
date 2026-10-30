package ru.agroeco.pulse.videoserver.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.videoserver.application.CreateVideoServerUseCase;
import ru.agroeco.pulse.videoserver.domain.VideoServer;
import ru.agroeco.pulse.videoserver.domain.VideoServerStatus;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.VideoServerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/videoserver/servers")
@RequiredArgsConstructor
@Tag(name = "Video Servers", description = "Управление видеосерверами (VMS)")
public class VideoServerController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name", "source", "status", "isActive", "createdAt", "updatedAt", "id");
    private static final String DEFAULT_SORT_FIELD = "name";

    private final CreateVideoServerUseCase createUseCase;
    private final VideoServerRepository repository;

    // ---------------- POST ----------------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create video server",
            description = "Пара (source, externalId) должна быть уникальна. "
                    + "При дубликате — 422 VIDEO_SERVER_ALREADY_EXISTS.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
                    @ApiResponse(responseCode = "422",
                            description = "Business rule violated (duplicate / invalid status)")
            }
    )
    public VideoServerResponse create(@Valid @RequestBody CreateVideoServerRequest request) {
        return createUseCase.handle(request);
    }

    // ---------------- GET ----------------

    @GetMapping
    @Operation(
            summary = "List video servers",
            description = "Фильтрация по source / objectId / zoneId / status / isActive / "
                    + "name (substring). Все фильтры опциональны. isActive по умолчанию "
                    + "НЕ применяется — передайте ?isActive=true|false явно, чтобы отфильтровать.",
            parameters = {
                    @Parameter(name = "sort", in = ParameterIn.QUERY,
                            description = "Sorting. Allowed fields: name, source, status, "
                                    + "isActive, createdAt, updatedAt, id",
                            example = "name,ASC"),
                    @Parameter(name = "page", in = ParameterIn.QUERY,
                            description = "Page number (0-based)", example = "0"),
                    @Parameter(name = "size", in = ParameterIn.QUERY,
                            description = "Page size (1..100)", example = "20")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Invalid sort/query param"),
                    @ApiResponse(responseCode = "422",
                            description = "Business rule violated (invalid status)")
            }
    )
    public PageResponse<VideoServerResponse> list(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) UUID objectId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = DEFAULT_SORT_FIELD, direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Pageable safePageable = sanitize(pageable);

        try {
            VideoServerStatus statusFilter = parseStatusFilter(status);
            Specification<VideoServer> spec = buildSpec(
                    source, objectId, zoneId, statusFilter, isActive, search);
            return PageResponse.of(
                    repository.findAll(spec, safePageable), this::toResponse);
        } catch (BusinessRuleException ex) {
            log.warn("Invalid query for video servers: {}", ex.getMessage());
            throw ex;
        } catch (DataAccessException ex) {
            log.error("DB error listing video servers: source={}, objectId={}, zoneId={}, "
                            + "status={}, isActive={}, search={}, pageable={}",
                    source, objectId, zoneId, status, isActive, search, safePageable, ex);
            throw ex;
        }
    }

    // ---------------- helpers ----------------

    private Pageable sanitize(Pageable pageable) {
        List<Sort.Order> valid = pageable.getSort().stream()
                .filter(o -> ALLOWED_SORT_FIELDS.contains(o.getProperty()))
                .toList();

        Sort sort = valid.isEmpty()
                ? Sort.by(Sort.Direction.ASC, DEFAULT_SORT_FIELD)
                : Sort.by(valid);

        if (valid.size() < pageable.getSort().stream().count()) {
            log.debug("Dropped unsupported sort properties, remaining: {}", sort);
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Specification<VideoServer> buildSpec(
            String source, UUID objectId, UUID zoneId,
            VideoServerStatus status, Boolean isActive, String search) {

        return (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (source != null && !source.isBlank()) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (objectId != null) {
                predicates.add(cb.equal(root.get("objectId"), objectId));
            }
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zoneId"), zoneId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern, '\\'));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Экранирует спецсимволы LIKE ({@code %}, {@code _}, {@code \}),
     * чтобы пользовательский ввод не превращался в wildcard-выражение.
     * Порядок важен: сначала экранируем сам escape-символ.
     */
    private String escapeLike(String s) {
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private VideoServerStatus parseStatusFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null; // фильтр не задан
        }
        try {
            return VideoServerStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException(
                    "INVALID_STATUS: '" + raw + "' (allowed: ONLINE, OFFLINE, UNKNOWN)");
        }
    }

    private VideoServerResponse toResponse(VideoServer s) {
        return new VideoServerResponse(
                s.getId(), s.getObjectId(), s.getZoneId(),
                s.getSource(), s.getExternalId(), s.getName(),
                s.getHost(), s.getBaseUrl(), s.getDescription(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}