package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.trassir.domain.AdminAuditLog;
import ru.agroeco.pulse.trassir.repository.AdminAuditLogRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirDataPurgeService {

    private final JdbcTemplate jdbc;
    private final AdminAuditLogRepository auditLogRepository;


    @Transactional
    public Map<String, Object> purgeAll() {
        Map<String, Long> before = countAll();
        jdbc.execute("""
        TRUNCATE TABLE
            trassir.events,
            trassir.ingest_raw,
            trassir.snapshot_parts,
            trassir.snapshot_objects,
            trassir.snapshots,
            trassir.snapshot_streams,
            trassir.objects,
            trassir.system_events,
            trassir.server_state,
            trassir.agent_heartbeats
        RESTART IDENTITY CASCADE
        """);

        String actor = currentActor();
        // FIX #85: Map вместо String-конкатенации.
        Map<String, Object> details = new LinkedHashMap<>(before);
        auditLogRepository.save(AdminAuditLog.builder()
                .actor(actor)
                .action("PURGE_ALL")
                .target("trassir")
                .details(details)
                .build());

        log.warn("TRASSIR admin purge: actor={} details={}", actor, details);

        Map<String, Object> result = new LinkedHashMap<>();
        before.forEach((k, v) -> result.put("deleted_" + k, v));
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAuditLog(int page, int size) {
        return auditLogRepository.findAll(PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * FIX #138: возвращаем AppPrincipal.id() (UUID-строка), чтобы
     * trassir.admin_audit_log.actor коррелировал с audit.audit_log.actor_id.
     * Раньше — username, который в audit.audit_log вообще не встречается.
     */
    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppPrincipal p)) {
            return "system";
        }
        return p.id().toString();
    }

    private Map<String, Long> countAll() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("events",            countOf("trassir.events"));
        counts.put("raw_batches",       countOf("trassir.ingest_raw"));
        counts.put("snapshot_parts",    countOf("trassir.snapshot_parts"));
        counts.put("snapshot_objects",  countOf("trassir.snapshot_objects"));
        counts.put("snapshots",         countOf("trassir.snapshots"));
        counts.put("snapshot_streams",  countOf("trassir.snapshot_streams"));
        counts.put("objects",           countOf("trassir.objects"));
        counts.put("system_events",     countOf("trassir.system_events"));
        counts.put("server_states",     countOf("trassir.server_state"));
        counts.put("heartbeats",        countOf("trassir.agent_heartbeats"));
        return counts;
    }

    private long countOf(String table) {
        Long v = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return v == null ? 0L : v;
    }

}