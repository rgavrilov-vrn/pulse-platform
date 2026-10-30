package ru.agroeco.pulse.platform.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature; // <-- ПРАВИЛЬНЫЙ ИМПОРТ

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                // Теперь константа находится в DateTimeFeature
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}