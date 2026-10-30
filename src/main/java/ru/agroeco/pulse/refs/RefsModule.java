package ru.agroeco.pulse.refs;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
        displayName = "Reference Data",
        allowedDependencies = {"platform", "audit"}
)
public interface RefsModule {
}