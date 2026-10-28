package ru.agroeco.pulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@SpringBootApplication(scanBasePackages = {
        "ru.agroeco.pulse"
})
@EnableJpaRepositories(basePackages = {
        "ru.agroeco.pulse"
})
@EntityScan(basePackages = {
        "ru.agroeco.pulse"
})
@EnableScheduling
public class PulseApplication {
    public static void main(String[] args) {
        System.out.println("APP MAIN STARTED");
        System.out.println(
                "Config class exists: " +
                        ru.agroeco.pulse.goodwan.GoodWanIngestionConfiguration.class.getName()
        );
        System.out.println("WEB APPLICATION TYPE: " +
                org.springframework.boot.WebApplicationType.deduce());
        SpringApplication.run(PulseApplication.class, args);
    }
}
