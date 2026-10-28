package ru.agroeco.pulse.videoserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.agroeco.pulse.outbox.api.OutboxMessage;
import ru.agroeco.pulse.outbox.api.OutboxPort;
import ru.agroeco.pulse.videoserver.api.RawVideoEventRequest;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.application.IngestRawVideoEventUseCase;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.NormalizedVideoEventRepository;
import ru.agroeco.pulse.videoserver.infrastructure.persistence.RawVideoEventRepository;

class VideoEventNormalizationTest {

    private final RawVideoEventRepository rawVideoEventRepository = mock(RawVideoEventRepository.class);
    private final NormalizedVideoEventRepository normalizedVideoEventRepository =
        mock(NormalizedVideoEventRepository.class);
    private final OutboxPort outboxPort = mock(OutboxPort.class);

    private final IngestRawVideoEventUseCase useCase = new IngestRawVideoEventUseCase(
        rawVideoEventRepository,
        normalizedVideoEventRepository,
        outboxPort,
        new ObjectMapper()
    );

    @Test
    void normalizesTamperingEventAndPublishesToOutbox() {
        RawVideoEventRequest request = new RawVideoEventRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-10-28T15:00:00Z"),
            "TRASSIR",
            "{\"event\":\"tampering\"}"
        );

        VideoEventNormalized result = useCase.handle(request);

        assertThat(result.eventType()).isEqualTo("CAMERA_TAMPERING");
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.payloadVersion()).isEqualTo(1);

        verify(rawVideoEventRepository).save(any());
        verify(normalizedVideoEventRepository).save(any());

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxPort).publish(captor.capture());

        OutboxMessage message = captor.getValue();
        assertThat(message.eventType()).isEqualTo("VideoEventNormalized");
        assertThat(message.entityId()).isEqualTo(result.eventId());
        assertThat(message.payload()).contains("CAMERA_TAMPERING");
    }
}
