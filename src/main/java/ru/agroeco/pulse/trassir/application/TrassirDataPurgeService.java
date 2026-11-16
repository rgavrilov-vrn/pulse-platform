package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.adapter.persistence.AdminAuditLogRepository;
import ru.agroeco.pulse.trassir.domain.AdminAuditLog;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrassirDataPurgeService {

    private final JdbcTemplate jdbc;
    private final AdminAuditLogRepository auditLogRepository;
    private final TrassirAdminAuditService auditService;

    @Transactional
    public Map<String, Object> purgeAll() {
        Map<String, Long> before = countAll();
        jdbc.execute("""
        TRUNCATE TABLE
            trassir.events, trassir.ingest_raw,
            trassir.snapshot_parts, trassir.snapshot_objects,
            trassir.snapshots, trassir.snapshot_streams,
            trassir.objects, trassir.system_events,
            trassir.server_state, trassir.agent_heartbeats
        RESTART IDENTITY CASCADE
        """);
        Map<String, Object> details = new LinkedHashMap<>(before);
        auditService.recordSuccess("PURGE_ALL", "trassir", details,
                before.values().stream().mapToInt(Long::intValue).sum());
        Map<String, Object> result = new LinkedHashMap<>();
        before.forEach((k, v) -> result.put("deleted_" + k, v));
        return result;
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAuditLog(int page, int size) {
        return auditLogRepository.findAll(PageRequest.of(
                Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
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