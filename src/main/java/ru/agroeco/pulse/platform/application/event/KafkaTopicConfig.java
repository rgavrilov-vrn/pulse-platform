package ru.agroeco.pulse.platform.application.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {
    @Bean
    public NewTopic incidentEventsTopic(@Value("${app.kafka.topic.incident-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
