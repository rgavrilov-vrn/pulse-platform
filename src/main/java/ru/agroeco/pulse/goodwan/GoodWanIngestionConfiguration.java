package ru.agroeco.pulse.goodwan;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class GoodWanIngestionConfiguration {

    @Bean
    public Clock goodWanClock() {
        return Clock.systemUTC();
    }
}
