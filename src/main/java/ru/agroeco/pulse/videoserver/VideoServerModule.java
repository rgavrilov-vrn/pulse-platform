package ru.agroeco.pulse.videoserver;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Video Server / TRASSIR",
        allowedDependencies = {"platform", "audit", "outbox", "trassir :: trassir"}
)
public interface VideoServerModule {}