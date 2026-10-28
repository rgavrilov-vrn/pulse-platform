package ru.agroeco.pulse.audit;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "Audit",
    allowedDependencies = {"platform"}
)
public interface AuditModule {
}
