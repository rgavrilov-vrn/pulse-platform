package ru.agroeco.pulse.platform.trassir.application;

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

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirDataPurgeService extends ru.agroeco.pulse.trassir.application.TrassirDataPurgeService {

    public TrassirDataPurgeService(
            TrassirEventRepository eventRepository,
            TrassirIngestRawRepository rawRepository,
            TrassirSnapshotPartRepository snapshotPartRepository,
            TrassirSnapshotObjectRepository snapshotObjectRepository,
            TrassirSnapshotRepository snapshotRepository,
            TrassirSnapshotStreamRepository snapshotStreamRepository,
            TrassirObjectRepository objectRepository,
            TrassirSystemEventRepository systemEventRepository,
            TrassirServerStateRepository serverStateRepository,
            TrassirAgentHeartbeatRepository agentHeartbeatRepository,
            AdminAuditLogRepository auditLogRepository
    ) {
        super(eventRepository, rawRepository, snapshotPartRepository, snapshotObjectRepository, snapshotRepository, snapshotStreamRepository, objectRepository, systemEventRepository, serverStateRepository, agentHeartbeatRepository, auditLogRepository);
    }
}
