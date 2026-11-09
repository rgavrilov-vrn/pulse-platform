package ru.agroeco.pulse.platform.trassir.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.agroeco.pulse.platform.trassir.application.ApplyAgentHeartbeatUseCase;
import ru.agroeco.pulse.platform.trassir.application.ApplyTrassirSystemEventUseCase;
import ru.agroeco.pulse.platform.trassir.application.ProcessTrassirObjectsSnapshotUseCase;
import ru.agroeco.pulse.platform.trassir.application.TrassirEventRouter;

import java.util.Map;

@Configuration
public class TrassirUseCaseConfig {

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
