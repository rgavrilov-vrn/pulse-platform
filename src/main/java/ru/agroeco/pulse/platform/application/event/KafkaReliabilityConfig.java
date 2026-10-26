package ru.agroeco.pulse.platform.application.event;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaReliabilityConfig {

    @Bean
    DefaultKafkaProducerFactoryCustomizer outboxProducerCustomizer() {
        return factory -> factory.updateConfigs(Map.of(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000));
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                new FixedBackOff(5000L, FixedBackOff.UNLIMITED_ATTEMPTS)
        );
    }
}