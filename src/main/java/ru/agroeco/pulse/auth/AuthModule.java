package ru.agroeco.pulse.auth;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Auth",
        allowedDependencies = {"platform", "audit"}
)
public interface AuthModule {}