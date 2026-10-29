package ru.agroeco.pulse.outbox.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.outbox.relay-enabled", havingValue = "true")
public class OutboxTopicConfig {

    @Bean
    NewTopic defaultOutboxTopic(
            @Value("${app.outbox.default-topic:pulse.events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic videoEventsTopic(
            @Value("${app.kafka.topic.video-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic incidentEventsTopic(
            @Value("${app.kafka.topic.incident-events}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic notificationRequestsTopic(
            @Value("${app.kafka.topic.notification-requests}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic notificationStatusTopic(
            @Value("${app.kafka.topic.notification-status}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}