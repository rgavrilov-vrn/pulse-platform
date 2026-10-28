package ru.agroeco.pulse.incidentorchestrator;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Incident Orchestrator",
    allowedDependencies = {"platform", "auth", "refs", "audit", "outbox", "incident", "notification"}
)
public interface IncidentOrchestratorModule {
}
