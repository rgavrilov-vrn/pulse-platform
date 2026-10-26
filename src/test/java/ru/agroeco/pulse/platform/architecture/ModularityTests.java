package ru.agroeco.pulse.platform.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(ru.agroeco.pulse.platform.PulseVideoEventsApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
