package ru.agroeco.pulse.incidentorchestrator;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Incident Orchestrator",
    allowedDependencies = {"platform", "auth", "refs", "audit", "outbox"}
)
public interface IncidentOrchestratorModule {
}
