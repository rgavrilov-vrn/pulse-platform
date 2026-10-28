package ru.agroeco.pulse.goodwan;

import org.springframework.modulith.ApplicationModule;

@ApplicationModule(
    displayName = "GoodWan Domain",
    allowedDependencies = {"platform", "auth", "refs", "audit", "outbox"}
)
public interface GoodwanModule {
}
