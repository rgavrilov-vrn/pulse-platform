package ru.agroeco.pulse.incidentorchestrator;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Incident Orchestrator",
        allowedDependencies = {
                "platform",
                "outbox :: outbox",
                "incident :: incident",     // ← теперь резолвится: incident.domain.event
                "notification :: notification"
                // убраны "auth", "refs", "audit" — в коде не импортируются
        }
)
public interface IncidentOrchestratorModule {}