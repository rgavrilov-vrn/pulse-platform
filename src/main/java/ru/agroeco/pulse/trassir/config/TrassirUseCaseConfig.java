package ru.agroeco.pulse.trassir.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.agroeco.pulse.trassir.application.TrassirMetrics;

@Configuration
public class TrassirUseCaseConfig {
    @Bean
    public TrassirMetrics trassirMetrics(MeterRegistry registry) {
        return new TrassirMetrics(registry);
    }
}