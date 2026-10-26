package ru.agroeco.pulse.videoevents.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import ru.agroeco.pulse.videoevents.adapter.persistence.outbox.OutboxEventEntity;
import ru.agroeco.pulse.videoevents.adapter.persistence.outbox.OutboxEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayUnitTest {
    @Mock
    OutboxEventRepository repository;
    @Mock
    KafkaTemplate<String, String> kafka;
    private OutboxRelay relay;
    private OutboxEventEntity event;

    @BeforeEach
    void setup() {
        relay = new OutboxRelay(repository, kafka, new ObjectMapper(), "events", 7, 2, 1);
        event = OutboxEventEntity.builder().id(UUID.randomUUID()).entityId(UUID.randomUUID())
                .eventType("IncidentCreated").payload("{}").status(OutboxStatus.NEW)
                .createdAt(Instant.now()).nextAttemptAt(Instant.now()).build();
        when(repository.lockPending(any(Instant.class), eq(7))).thenReturn(List.of(event));
    }

    @Test
    void acknowledgedEventBecomesSent() {
        when(kafka.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));
        relay.publishPendingEvents();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(repository).flush();
    }

    @Test
    void failedFutureSchedulesRetry() {
        when(kafka.send(any(Message.class))).thenReturn(CompletableFuture.<SendResult<String, String>>
                failedFuture(new IllegalStateException("offline")));
        relay.publishPendingEvents();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
    }

    @Test
    void exhaustedRetriesBecomeFailed() {
        event.setAttempts(1);
        when(kafka.send(any(Message.class))).thenReturn(CompletableFuture.<SendResult<String, String>>
                failedFuture(new IllegalStateException("offline")));
        relay.publishPendingEvents();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(2);
    }

    @Test
    void timeoutDoesNotMarkSent() {
        when(kafka.send(any(Message.class))).thenReturn(new CompletableFuture<>());
        relay.publishPendingEvents();
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNull();
    }
}
