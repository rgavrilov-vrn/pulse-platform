package ru.agroeco.pulse.platform.trassir.application;

import ru.agroeco.pulse.trassir.repository.TrassirSystemEventRepository;
import tools.jackson.databind.ObjectMapper;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class ApplyTrassirSystemEventUseCase extends ru.agroeco.pulse.trassir.application.ApplyTrassirSystemEventUseCase {

    public ApplyTrassirSystemEventUseCase(
            TrassirSystemEventRepository systemEventRepository,
            ObjectMapper objectMapper
    ) {
        super(systemEventRepository, objectMapper);
    }
}
