package ru.agroeco.pulse.trassir.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.agroeco.pulse.trassir.application.ApplyAgentHeartbeatUseCase;
import ru.agroeco.pulse.trassir.application.ApplyTrassirSystemEventUseCase;
import ru.agroeco.pulse.trassir.application.ProcessTrassirObjectsSnapshotUseCase;
import ru.agroeco.pulse.trassir.application.TrassirEventRouter;
import ru.agroeco.pulse.trassir.application.TrassirMetrics;

import java.util.Map;

@Configuration
public class TrassirUseCaseConfig {

    @Bean
    public TrassirMetrics trassirMetrics(MeterRegistry registry) {
        return new TrassirMetrics(registry);
    }

    @Bean
    public TrassirEventRouter trassirEventRouter(
            ApplyAgentHeartbeatUseCase heartbeat,
            ProcessTrassirObjectsSnapshotUseCase snapshot,
            ApplyTrassirSystemEventUseCase systemEvent
    ) {
        return new TrassirEventRouter(Map.of(
                "agent.heartbeat", heartbeat,
                "agent.objects_snapshot", snapshot,
                "trassir.system_event", systemEvent
        ));
    }
}
