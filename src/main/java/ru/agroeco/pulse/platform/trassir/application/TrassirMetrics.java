package ru.agroeco.pulse.platform.trassir.application;

import io.micrometer.core.instrument.MeterRegistry;

/** Temporary non-bean compatibility subclass; remove after migration. */
@Deprecated
public class TrassirMetrics extends ru.agroeco.pulse.trassir.application.TrassirMetrics {

    public TrassirMetrics(MeterRegistry registry) {
        super(registry);
    }
}
