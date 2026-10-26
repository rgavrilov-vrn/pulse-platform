package ru.agroeco.pulse.videoevents.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(ru.agroeco.pulse.videoevents.PulseVideoEventsApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
