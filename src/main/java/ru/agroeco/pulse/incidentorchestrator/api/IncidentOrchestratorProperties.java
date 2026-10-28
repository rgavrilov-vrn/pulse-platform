package ru.agroeco.pulse.incidentorchestrator.api;

import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pulse.orchestrator")
public class IncidentOrchestratorProperties {

    private UUID defaultRecipientId;
    private String defaultRole = "SITE_OPERATOR";
    private String criticalChannel = "MESSENGER";
    private String defaultChannel = "PULSE_UI";
    private Duration criticalReactionSla = Duration.ofMinutes(15);
    private Duration defaultReactionSla = Duration.ofHours(4);
}
