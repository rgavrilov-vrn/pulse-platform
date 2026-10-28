package ru.agroeco.pulse.outbox;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Outbox",
    allowedDependencies = {"platform"}
)
public interface OutboxModule {
}
