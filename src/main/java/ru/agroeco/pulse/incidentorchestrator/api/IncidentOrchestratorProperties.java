package ru.agroeco.pulse.incidentorchestrator.api;

import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pulse.orchestrator")
public class IncidentOrchestratorProperties {

    private UUID defaultRecipientId;
    private String defaultRole = "SITE_OPERATOR";
    private String criticalChannel = "MESSENGER";
    private String defaultChannel = "PULSE_UI";
    private Duration criticalReactionSla = Duration.ofMinutes(15);
    private Duration defaultReactionSla = Duration.ofHours(4);

    public UUID getDefaultRecipientId() {
        return defaultRecipientId;
    }

    public void setDefaultRecipientId(UUID defaultRecipientId) {
        this.defaultRecipientId = defaultRecipientId;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public String getCriticalChannel() {
        return criticalChannel;
    }

    public void setCriticalChannel(String criticalChannel) {
        this.criticalChannel = criticalChannel;
    }

    public String getDefaultChannel() {
        return defaultChannel;
    }

    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }

    public Duration getCriticalReactionSla() {
        return criticalReactionSla;
    }

    public void setCriticalReactionSla(Duration criticalReactionSla) {
        this.criticalReactionSla = criticalReactionSla;
    }

    public Duration getDefaultReactionSla() {
        return defaultReactionSla;
    }

    public void setDefaultReactionSla(Duration defaultReactionSla) {
        this.defaultReactionSla = defaultReactionSla;
    }
}
