package ru.agroeco.pulse.platform.adapter.http;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final BuildProperties buildProperties;
    private final Environment environment;

    @Bean
    public OpenAPI openAPI() {
        // FIX #91: title/version из build-info (pom), а не hardcoded.
        // Раньше — "Pulse Video Events API 0.9.0" при artifactId=pulse-platform
        // и version=1.0-SNAPSHOT.
        String title = "Pulse Platform API";
        String version = buildProperties.getVersion() != null
                ? buildProperties.getVersion()
                : "unknown";
        String description = "Incident management, video events, TRASSIR telemetry, auth, refs.";
        if (environment.matchesProfiles("local")) {
            description += " (local profile — dev-only credentials in examples)";
        }

        return new OpenAPI()
                .info(new Info().title(title).version(version).description(description))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}