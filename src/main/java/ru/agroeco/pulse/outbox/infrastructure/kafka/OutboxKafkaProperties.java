package ru.agroeco.pulse.outbox.infrastructure.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxKafkaProperties {
    private String defaultTopic = "pulse.events";
    private int batchSize = 50;
    private int maxAttempts = 5;
    private Map<String, String> eventTopics = new HashMap<>(); // eventType -> topic
}
