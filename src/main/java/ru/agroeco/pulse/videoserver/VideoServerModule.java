package ru.agroeco.pulse.videoserver;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Video Server / TRASSIR",
    allowedDependencies = {"platform", "auth", "refs", "audit", "outbox"}
)
public interface VideoServerModule {
}
