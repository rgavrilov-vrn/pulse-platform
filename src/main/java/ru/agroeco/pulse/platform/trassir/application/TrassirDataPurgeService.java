package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.domain.AdminAuditLog;
import ru.agroeco.pulse.platform.trassir.repository.AdminAuditLogRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirIngestRawRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirDataPurgeService {

    private final TrassirEventRepository eventRepository;
    private final TrassirIngestRawRepository rawRepository;
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public Map<String, Object> purgeAll() {
        int deletedEvents = eventRepository.deleteAllEvents();
        int deletedRaw = rawRepository.deleteAllRaw();

        String actor = currentActor();
        String details = "{\"deletedEvents\":" + deletedEvents + ",\"deletedRawBatches\":" + deletedRaw + "}";

        auditLogRepository.save(AdminAuditLog.builder()
                .actor(actor)
                .action("PURGE_ALL")
                .target("trassir")
                .details(details)
                .build());

        log.warn("TRASSIR admin purge: actor={} deleted {} events and {} raw batches", actor, deletedEvents, deletedRaw);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedEvents", deletedEvents);
        result.put("deletedRawBatches", deletedRaw);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAuditLog() {
        return auditLogRepository.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
