package ru.agroeco.pulse.platform.trassir.application;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Temporary Spring adapter while callers migrate to the trassir module. */
@Component
public class TrassirMetrics extends ru.agroeco.pulse.trassir.application.TrassirMetrics {

    public TrassirMetrics(MeterRegistry registry) {
        super(registry);
    }
}
