package ru.agroeco.pulse.trassir.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.api.dto.PageResponse;
import ru.agroeco.pulse.platform.api.support.QuerySupport;
import ru.agroeco.pulse.trassir.application.TrassirDataPurgeService;
import ru.agroeco.pulse.trassir.application.dto.AdminAuditLogView;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trassir/admin")
@RequiredArgsConstructor
public class TrassirAdminDataController {

    private final TrassirDataPurgeService purgeService;

    @DeleteMapping("/data")
    @PreAuthorize("hasAuthority('trassir:admin')")
    public Map<String, Object> purgeAll() {
        return purgeService.purgeAll();
    }

    /**
     * FIX #145: было {@code ResponseEntity<?>} с {@code Page<AdminAuditLog>} —
     * наружу уходили entity + служебные поля PageImpl. Теперь — DTO + PageResponse.
     * FIX #26: @PreAuthorize (SecurityConfig уже закрывает GET /admin/**,
     * но defense-in-depth — на методе).
     */
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('trassir:admin')")
    public PageResponse<AdminAuditLogView> auditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<AdminAuditLogView> result = purgeService.getAuditLog(page, size)
                .map(e -> new AdminAuditLogView(
                        e.getId(), e.getActor(), e.getAction(), e.getTarget(),
                        e.getDetails(), e.getCreatedAt()));
        return PageResponse.of(result);
    }
}