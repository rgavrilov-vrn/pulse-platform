package ru.agroeco.pulse.refs;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Reference Data",
        allowedDependencies = {"platform"}  // ← убрать "audit": не используется
)
public interface RefsModule {}