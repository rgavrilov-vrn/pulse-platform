package ru.agroeco.pulse.platform.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import ru.agroeco.pulse.platform.PulsePlatformApplication;

class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(PulsePlatformApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
