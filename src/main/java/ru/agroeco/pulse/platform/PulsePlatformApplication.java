package ru.agroeco.pulse.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic
@SpringBootApplication(scanBasePackages = {
        "ru.agroeco.pulse.platform",
        "ru.agroeco.pulse.goodwan"
})
@EnableJpaRepositories(basePackages = {
        "ru.agroeco.pulse.platform",
        "ru.agroeco.pulse.goodwan"
})
@EntityScan(basePackages = {
        "ru.agroeco.pulse.platform",
        "ru.agroeco.pulse.goodwan"
})
@EnableScheduling
public class PulsePlatformApplication {
    public static void main(String[] args) {
        System.out.println("APP MAIN STARTED");
        System.out.println(
                "Config class exists: " +
                        ru.agroeco.pulse.goodwan.GoodWanIngestionConfiguration.class.getName()
        );
        System.out.println("WEB APPLICATION TYPE: " +
                org.springframework.boot.WebApplicationType.deduce());
        SpringApplication.run(PulsePlatformApplication.class, args);
    }
}
