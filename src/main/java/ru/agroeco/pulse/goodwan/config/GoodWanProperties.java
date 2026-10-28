package ru.agroeco.pulse.goodwan.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "goodwan")
public record GoodWanProperties(
        String baseUrl,
        String username,
        String password,
        Sync sync,
        List<String> devices
) {

    public GoodWanProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("goodwan.base-url is required");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("goodwan.username is required");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("goodwan.password is required");
        }

        if (sync == null) {
            sync = new Sync(true, Duration.ofMinutes(1), 1000);
        }

        if (devices == null) {
            devices = List.of();
        }
    }

    public record Sync(
            boolean enabled,
            Duration interval,
            Integer pageSize
    ) {

        public Sync {
            if (interval == null) {
                interval = Duration.ofMinutes(1);
            }

            if (pageSize == null || pageSize <= 0) {
                pageSize = 1000;
            }
        }
    }
}