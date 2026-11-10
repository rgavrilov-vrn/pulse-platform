package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.domain.AdminAuditLog;
import ru.agroeco.pulse.trassir.repository.AdminAuditLogRepository;
import ru.agroeco.pulse.trassir.repository.TrassirAgentHeartbeatRepository;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.trassir.repository.TrassirIngestRawRepository;
import ru.agroeco.pulse.trassir.repository.TrassirObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirServerStateRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotPartRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotStreamRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSystemEventRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirDataPurgeService {

    private final TrassirEventRepository eventRepository;
    private final TrassirIngestRawRepository rawRepository;
    private final TrassirSnapshotPartRepository snapshotPartRepository;
    private final TrassirSnapshotObjectRepository snapshotObjectRepository;
    private final TrassirSnapshotRepository snapshotRepository;
    private final TrassirSnapshotStreamRepository snapshotStreamRepository;
    private final TrassirObjectRepository objectRepository;
    private final TrassirSystemEventRepository systemEventRepository;
    private final TrassirServerStateRepository serverStateRepository;
    private final TrassirAgentHeartbeatRepository agentHeartbeatRepository;
    private final AdminAuditLogRepository auditLogRepository;

    @Transactional
    public Map<String, Object> purgeAll() {
        int deletedEvents = eventRepository.deleteAllEvents();
        int deletedRaw = rawRepository.deleteAllRaw();
        int deletedSnapshotParts = snapshotPartRepository.deleteAllParts();
        int deletedSnapshotObjects = snapshotObjectRepository.deleteAllObjects();
        int deletedSnapshots = snapshotRepository.deleteAllSnapshots();
        int deletedSnapshotStreams = snapshotStreamRepository.deleteAllStreams();
        int deletedObjects = objectRepository.deleteAllObjects();
        int deletedSystemEvents = systemEventRepository.deleteAllSystemEvents();
        int deletedServerStates = serverStateRepository.deleteAllServerStates();
        int deletedHeartbeats = agentHeartbeatRepository.deleteAllHeartbeats();

        String actor = currentActor();
        String details = "{\"deletedEvents\":" + deletedEvents
                + ",\"deletedRawBatches\":" + deletedRaw
                + ",\"deletedSnapshotParts\":" + deletedSnapshotParts
                + ",\"deletedSnapshotObjects\":" + deletedSnapshotObjects
                + ",\"deletedSnapshots\":" + deletedSnapshots
                + ",\"deletedSnapshotStreams\":" + deletedSnapshotStreams
                + ",\"deletedObjects\":" + deletedObjects
                + ",\"deletedSystemEvents\":" + deletedSystemEvents
                + ",\"deletedServerStates\":" + deletedServerStates
                + ",\"deletedHeartbeats\":" + deletedHeartbeats + "}";

        auditLogRepository.save(AdminAuditLog.builder()
                .actor(actor)
                .action("PURGE_ALL")
                .target("trassir")
                .details(details)
                .build());

        log.warn("TRASSIR admin purge: actor={} details={}", actor, details);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedEvents", deletedEvents);
        result.put("deletedRawBatches", deletedRaw);
        result.put("deletedSnapshotParts", deletedSnapshotParts);
        result.put("deletedSnapshotObjects", deletedSnapshotObjects);
        result.put("deletedSnapshots", deletedSnapshots);
        result.put("deletedSnapshotStreams", deletedSnapshotStreams);
        result.put("deletedObjects", deletedObjects);
        result.put("deletedSystemEvents", deletedSystemEvents);
        result.put("deletedServerStates", deletedServerStates);
        result.put("deletedHeartbeats", deletedHeartbeats);
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
