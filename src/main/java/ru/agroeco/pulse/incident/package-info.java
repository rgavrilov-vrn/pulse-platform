@ApplicationModule(
        displayName = "Incident",
        allowedDependencies = {"platform", "auth", "videoserver::api"}
)
package ru.agroeco.pulse.incident;

import org.springframework.modulith.ApplicationModule;