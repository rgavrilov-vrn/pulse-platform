package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.application.TrassirMetrics;
import ru.agroeco.pulse.trassir.repository.TrassirSnapshotRepository;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirSnapshotRetryWorker extends ru.agroeco.pulse.trassir.application.TrassirSnapshotRetryWorker {

    public TrassirSnapshotRetryWorker(
            TrassirSnapshotRepository snapshotRepository,
            TrassirMetrics metrics
    ) {
        super(snapshotRepository, metrics);
    }
}
