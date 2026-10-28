package ru.agroeco.pulse.refs;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "References",
    allowedDependencies = {"platform"}
)
public interface RefsModule {
}
