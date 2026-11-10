package ru.agroeco.pulse.incident;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Incident",
        // FIX #14: CreateIncidentFromVideoEventUseCase импортирует
        // ru.agroeco.pulse.videoserver.api.VideoEventNormalized / EventSeverity.
        // Без этого SpringModulith.verify() падает с DependencyViolation.
        allowedDependencies = {"platform", "auth", "audit", "videoserver::api"}
)
public interface IncidentModule {}