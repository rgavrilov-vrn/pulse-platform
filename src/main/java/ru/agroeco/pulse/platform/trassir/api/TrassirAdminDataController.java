package ru.agroeco.pulse.platform.trassir.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.agroeco.pulse.trassir.application.TrassirDataPurgeService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/trassir/admin")
@RequiredArgsConstructor
public class TrassirAdminDataController {

    private final TrassirDataPurgeService purgeService;

    @DeleteMapping("/data")
    public ResponseEntity<Map<String, Object>> purgeAll() {
        Map<String, Object> result = purgeService.purgeAll();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit")
    public ResponseEntity<?> auditLog() {
        return ResponseEntity.ok(purgeService.getAuditLog());
    }
}
