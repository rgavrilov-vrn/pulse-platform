package ru.agroeco.pulse.platform.trassir.application;

import org.springframework.transaction.PlatformTransactionManager;
import ru.agroeco.pulse.trassir.application.TrassirEventRouter;
import ru.agroeco.pulse.trassir.application.TrassirMetrics;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import tools.jackson.databind.ObjectMapper;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirEventProcessor extends ru.agroeco.pulse.trassir.application.TrassirEventProcessor {

    public TrassirEventProcessor(
            TrassirEventRepository eventRepository,
            TrassirEventRouter router,
            TrassirMetrics metrics,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        super(eventRepository, router, metrics, objectMapper, transactionManager);
    }
}
