package ru.agroeco.pulse.goodwan.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoodWanProperties.class)
public class GoodWanClientConfiguration {

    @Bean
    public RestClient goodWanRestClient(GoodWanProperties properties) {
        String credentials = properties.username() + ":" + properties.password();
        String encodedCredentials = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic " + encodedCredentials
            )
            .build();
    }
}
