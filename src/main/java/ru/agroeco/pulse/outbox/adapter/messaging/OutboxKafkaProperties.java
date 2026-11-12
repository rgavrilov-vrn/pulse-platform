package ru.agroeco.pulse.outbox.adapter.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.outbox")
@EnableConfigurationProperties(OutboxKafkaProperties.class)
public class OutboxKafkaProperties {
    private String defaultTopic = "pulse.events";
    private int batchSize = 50;
    private int maxAttempts = 5;
    private Map<String, String> eventTopics = new HashMap<>(); // eventType -> topic
}
