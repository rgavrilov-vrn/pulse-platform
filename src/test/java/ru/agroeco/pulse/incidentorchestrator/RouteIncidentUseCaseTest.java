package ru.agroeco.pulse.incidentorchestrator;

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
import ru.agroeco.pulse.incidentorchestrator.api.IncidentOrchestratorProperties;
import ru.agroeco.pulse.incidentorchestrator.application.RouteIncidentUseCase;
import ru.agroeco.pulse.notification.api.NotificationRequested;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;

class RouteIncidentUseCaseTest {

    private final OutboxPort outboxPort = mock(OutboxPort.class);
    private final IncidentOrchestratorProperties properties = new IncidentOrchestratorProperties();

    private final RouteIncidentUseCase useCase = new RouteIncidentUseCase(
        properties,
        outboxPort,
        new ObjectMapper()
    );

    @Test
    void routesCriticalIncidentToMessengerWithHighPriority() {
        IncidentCreated event = new IncidentCreated(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "VideoEventNormalized",
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "CRITICAL",
            "Video incident: CAMERA_TAMPERING",
            Instant.parse("2026-10-28T15:00:00Z")
        );

        NotificationRequested result = useCase.handle(event);

        assertThat(result.channel()).isEqualTo("MESSENGER");
        assertThat(result.priority()).isEqualTo("HIGH");
        assertThat(result.acknowledgementRequired()).isTrue();
        assertThat(result.templateCode()).isEqualTo("critical-incident-created");

        verify(outboxPort).publish(any());
    }

    @Test
    void routesNormalIncidentToPulseUi() {
        IncidentCreated event = new IncidentCreated(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "VideoEventNormalized",
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "WARNING",
            "Video incident: MOTION_DETECTED",
            Instant.parse("2026-10-28T15:00:00Z")
        );

        NotificationRequested result = useCase.handle(event);

        assertThat(result.channel()).isEqualTo("PULSE_UI");
        assertThat(result.priority()).isEqualTo("NORMAL");
        assertThat(result.acknowledgementRequired()).isFalse();
        assertThat(result.templateCode()).isEqualTo("incident-created");

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort).publish(captor.capture());

        assertThat(captor.getValue().eventType()).isEqualTo("NotificationRequested");
    }
}
