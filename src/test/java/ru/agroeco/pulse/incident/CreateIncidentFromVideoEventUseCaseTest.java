package ru.agroeco.pulse.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.agroeco.pulse.incident.api.IncidentCreated;
import ru.agroeco.pulse.incident.application.CreateIncidentFromVideoEventUseCase;
import ru.agroeco.pulse.incident.infrastructure.persistence.IncidentRepository;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;

class CreateIncidentFromVideoEventUseCaseTest {

    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final OutboxPort outboxPort = mock(OutboxPort.class);

    private final CreateIncidentFromVideoEventUseCase useCase =
        new CreateIncidentFromVideoEventUseCase(
            incidentRepository,
            outboxPort,
            new ObjectMapper()
        );

    @Test
    void createsIncidentAndPublishesIncidentCreated() {
        VideoEventNormalized event = new VideoEventNormalized(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            Instant.parse("2026-10-28T15:00:00Z"),
            "TRASSIR",
            "CAMERA_TAMPERING",
            "CRITICAL",
            1
        );

        IncidentCreated result = useCase.handle(event);

        assertThat(result.sourceEventId()).isEqualTo(event.eventId());
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.title()).contains("CAMERA_TAMPERING");

        verify(incidentRepository).save(any());

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort).publish(captor.capture());

        OutboxMessage message = captor.getValue();
        assertThat(message.eventType()).isEqualTo("IncidentCreated");
        assertThat(message.entityId()).isEqualTo(result.incidentId());
        assertThat(message.payload()).contains("IncidentCreated");
    }
}
