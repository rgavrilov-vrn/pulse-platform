package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.application.TrassirEventRouter;
import ru.agroeco.pulse.trassir.repository.TrassirObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotObjectRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotPartRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotStreamRepository;
import tools.jackson.databind.ObjectMapper;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class ProcessTrassirObjectsSnapshotUseCase extends ru.agroeco.pulse.trassir.application.ProcessTrassirObjectsSnapshotUseCase {

    public ProcessTrassirObjectsSnapshotUseCase(
            TrassirSnapshotRepository snapshotRepository,
            TrassirSnapshotPartRepository partRepository,
            TrassirSnapshotStreamRepository streamRepository,
            TrassirSnapshotObjectRepository snapshotObjectRepository,
            TrassirObjectRepository objectRepository,
            ObjectMapper objectMapper
    ) {
        super(snapshotRepository, partRepository, streamRepository, snapshotObjectRepository, objectRepository, objectMapper);
    }
}
