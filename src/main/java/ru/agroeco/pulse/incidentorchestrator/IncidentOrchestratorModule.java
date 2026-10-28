package ru.agroeco.pulse.incidentorchestrator;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Incident Orchestrator",
    allowedDependencies = {
        "platform",
        "auth",
        "refs",
        "audit",
        "outbox :: outbox",
        "incident :: incident",
        "notification :: notification"
    }
)
public interface IncidentOrchestratorModule {
}
