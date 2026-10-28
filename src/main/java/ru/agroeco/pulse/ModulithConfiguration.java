package ru.agroeco.pulse;

import org.springframework.modulith.Modulith;

@Modulith(
    systemName = "Pulse Platform",
    sharedModules = {
        "platform",
        "auth",
        "refs",
        "audit",
        "outbox"
    }
)
public final class ModulithConfiguration {
    private ModulithConfiguration() {
    }
}
