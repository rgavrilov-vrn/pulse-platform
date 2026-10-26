package ru.agroeco.pulse.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
        "ru.agroeco.pulse.platform",
        "ru.agroeco.pulse.goodwan"
})public class PulsePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulsePlatformApplication.class, args);
    }
}
