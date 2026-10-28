package ru.agroeco.pulse.incident;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Incident Management",
    allowedDependencies = {
        "platform",
        "auth",
        "refs",
        "audit",
        "outbox :: outbox",
        "videoserver :: videoserver"
    }
)
public interface IncidentModule {
}
