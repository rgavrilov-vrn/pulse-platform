package ru.agroeco.pulse.incident;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Incident",
        allowedDependencies = {"platform", "auth", "audit"}
)
public interface IncidentModule {}