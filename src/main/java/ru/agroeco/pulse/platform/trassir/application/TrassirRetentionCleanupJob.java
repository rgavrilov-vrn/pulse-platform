package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.trassir.repository.TrassirIngestRawRepository;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirRetentionCleanupJob extends ru.agroeco.pulse.trassir.application.TrassirRetentionCleanupJob {

    public TrassirRetentionCleanupJob(
            TrassirIngestRawRepository rawRepository,
            TrassirEventRepository eventRepository
    ) {
        super(rawRepository, eventRepository);
    }
}
