package ru.agroeco.pulse.outbox.infrastructure.kafka;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.outbox.relay-enabled", havingValue = "true")
public class OutboxProducerConfig {

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
}