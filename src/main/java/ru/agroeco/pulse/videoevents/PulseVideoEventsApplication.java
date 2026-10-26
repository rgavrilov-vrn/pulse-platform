package ru.agroeco.pulse.videoevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulithic
@EnableScheduling
@SpringBootApplication
public class PulseVideoEventsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PulseVideoEventsApplication.class, args);
    }
}
