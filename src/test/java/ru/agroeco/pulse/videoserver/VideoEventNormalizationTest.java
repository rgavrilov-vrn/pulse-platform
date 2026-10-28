package ru.agroeco.pulse.videoserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.agroeco.pulse.videoserver.api.RawVideoEventRequest;
import ru.agroeco.pulse.videoserver.api.VideoEventNormalized;
import ru.agroeco.pulse.videoserver.application.IngestRawVideoEventUseCase;

@SpringBootTest
class VideoEventNormalizationTest {

    @Autowired
    private IngestRawVideoEventUseCase ingestRawVideoEventUseCase;

    @Test
    void normalizesTamperingEventAsCritical() {
        RawVideoEventRequest request = new RawVideoEventRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-10-28T15:00:00Z"),
            "TRASSIR",
            "{\"event\":\"tampering\"}"
        );

        VideoEventNormalized result = ingestRawVideoEventUseCase.handle(request);

        assertThat(result.eventType()).isEqualTo("CAMERA_TAMPERING");
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.payloadVersion()).isEqualTo(1);
        assertThat(result.rawEventId()).isNotNull();
    }
}
