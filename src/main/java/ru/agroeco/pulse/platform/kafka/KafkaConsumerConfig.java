package ru.agroeco.pulse.platform.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    /**
     * FIX #55: DLT для poison-pill. Spring Boot использует этот бин для
     * дефолтного kafkaListenerContainerFactory.
     *
     * <p>Стратегия: 3 ретрая с интервалом 1 с; если не помогло — отправка
     * в `<topic>.dlt`. Не должны ретраиться: ошибки десериализации и
     * IllegalArgumentException (обычно — ошибка контракта).</p>
     */
    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler(KafkaTemplate<String, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".dlt", -1));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(
                ClassCastException.class,
                IllegalArgumentException.class,
                tools.jackson.core.JacksonException.class);
        handler.setLogLevel(org.springframework.kafka.KafkaException.Level.WARN);
        return handler;
    }
}