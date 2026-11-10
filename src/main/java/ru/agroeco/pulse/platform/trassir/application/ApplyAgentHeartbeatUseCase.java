package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.repository.TrassirAgentHeartbeatRepository;
import ru.agroeco.pulse.trassir.repository.TrassirServerStateRepository;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class ApplyAgentHeartbeatUseCase extends ru.agroeco.pulse.trassir.application.ApplyAgentHeartbeatUseCase {

    public ApplyAgentHeartbeatUseCase(
            TrassirAgentHeartbeatRepository heartbeatRepository,
            TrassirServerStateRepository serverStateRepository
    ) {
        super(heartbeatRepository, serverStateRepository);
    }
}
