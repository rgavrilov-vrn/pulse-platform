package ru.agroeco.pulse.auth.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.login-rate-limit")
public class LoginRateLimiterProperties {

    /** Сколько попыток разрешено в окне. */
    private int maxAttempts = 10;

    /** Размер окна. */
    private Duration window = Duration.ofMinutes(5);
}