package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.application.TrassirEventRouter;
import ru.agroeco.pulse.trassir.application.TrassirMetrics;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import tools.jackson.databind.ObjectMapper;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirEventRetryWorker extends ru.agroeco.pulse.trassir.application.TrassirEventRetryWorker {

    public TrassirEventRetryWorker(
            TrassirEventRepository eventRepository,
            TrassirEventRouter router,
            TrassirMetrics metrics,
            ObjectMapper objectMapper
    ) {
        super(eventRepository, router, metrics, objectMapper);
    }
}
