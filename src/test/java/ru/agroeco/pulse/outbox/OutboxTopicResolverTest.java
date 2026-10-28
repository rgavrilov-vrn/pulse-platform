package ru.agroeco.pulse.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import ru.agroeco.pulse.outbox.infrastructure.kafka.OutboxKafkaProperties;
import ru.agroeco.pulse.outbox.infrastructure.kafka.OutboxKafkaRelay;
import ru.agroeco.pulse.platform.adapter.persistence.outbox.OutboxEventRepository;

class OutboxTopicResolverTest {

    private final OutboxKafkaRelay relay = new OutboxKafkaRelay(
        mock(OutboxEventRepository.class),
        mock(KafkaTemplate.class),
        new OutboxKafkaProperties()
    );

    @Test
    void resolvesVideoServerTopic() {
        assertThat(relay.resolveTopic("VideoEventNormalized"))
            .isEqualTo("pulse.videoserver.events");
    }

    @Test
    void resolvesIncidentTopic() {
        assertThat(relay.resolveTopic("IncidentCreated"))
            .isEqualTo("pulse.incident.events");
    }

    @Test
    void resolvesDefaultTopicForUnknownEvent() {
        assertThat(relay.resolveTopic("UnknownEvent"))
            .isEqualTo("pulse.events");
    }
}
