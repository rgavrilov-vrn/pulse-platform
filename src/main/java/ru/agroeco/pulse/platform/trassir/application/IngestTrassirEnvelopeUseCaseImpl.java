package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirIngestRawRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IngestTrassirEnvelopeUseCaseImpl implements IngestTrassirEnvelopeUseCase {

    private final TrassirEnvelopeParser parser;
    private final TrassirEventRouter router;
    private final TrassirIngestRawRepository rawRepository;
    private final TrassirEventRepository eventRepository;

    @Override
    @Transactional
    public void handle(String rawBody) {
        Instant receivedAt = Instant.now();
        ParsedTrassirEnvelope envelope = parser.parse(rawBody);

        TrassirIngestRaw raw = new TrassirIngestRaw();
        raw.setBatchId(envelope.batchId());
        raw.setReceivedAt(receivedAt);
        raw.setSentAt(envelope.sentAt());
        raw.setProducerName(envelope.producer() != null ? envelope.producer().name() : null);
        raw.setProducerVersion(envelope.producer() != null ? envelope.producer().version() : null);
        raw.setServerId(envelope.server().serverId());
        raw.setRawBody(rawBody);
        raw = rawRepository.save(raw);

        int index = 0;
        for (ParsedTrassirEnvelope.Event event : envelope.events()) {
            TrassirEvent entity = new TrassirEvent();
            entity.setRawId(raw.getId());
            entity.setEventIndex(index++);
            entity.setEventId(event.eventId());
            entity.setEventType(event.eventType());
            entity.setEventVersion(event.eventVersion());
            entity.setOccurredAt(event.occurredAt());
            entity.setObservedAt(event.observedAt());
            entity.setSource(event.source());
            entity.setData(event.data());
            entity.setRawEvent(event.data());
            entity = eventRepository.save(entity);

            TrassirEventContext context = new TrassirEventContext(
                    envelope.server().serverId(),
                    envelope.batchId(),
                    receivedAt,
                    event
            );

            EventProcessingResult result = router.route(context);
            if (!result.success()) {
                entity.setProcessingError(result.error());
                eventRepository.save(entity);
            }
        }
    }
}
