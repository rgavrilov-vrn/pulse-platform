package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.domain.TrassirEvent;
import ru.agroeco.pulse.platform.trassir.domain.TrassirIngestRaw;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirIngestRawRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestTrassirEnvelopeUseCaseImpl implements IngestTrassirEnvelopeUseCase {

    private final TrassirEnvelopeParser parser;
    private final TrassirIngestRawRepository rawRepository;
    private final TrassirEventRepository eventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TrassirMetrics metrics;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void handle(String rawBody) {
        Timer.Sample sample = metrics.startIngest();

        try {
            Instant receivedAt = Instant.now();
            ParsedTrassirEnvelope envelope = parser.parse(rawBody);

            if (rawRepository.existsByBatchId(envelope.batchId())) {
                metrics.incrementIngest("duplicate");
                return;
            }

            TrassirIngestRaw raw = new TrassirIngestRaw();
            raw.setBatchId(envelope.batchId());
            raw.setReceivedAt(receivedAt);
            raw.setSentAt(envelope.sentAt());
            raw.setProducerName(envelope.producer() != null ? envelope.producer().name() : null);
            raw.setProducerVersion(envelope.producer() != null ? envelope.producer().version() : null);
            raw.setServerId(envelope.server().serverId());
            raw.setRawBody(rawBody);
            raw = rawRepository.save(raw);

            List<UUID> eventIds = new ArrayList<>();
            int index = 0;

            for (ParsedTrassirEnvelope.Event event : envelope.events()) {
                TrassirEvent entity = new TrassirEvent();
                entity.setRawId(raw.getId());
                entity.setServerId(envelope.server().serverId());
                entity.setEventIndex(index++);
                entity.setEventId(event.eventId());
                entity.setEventType(event.eventType());
                entity.setEventVersion(event.eventVersion());
                entity.setOccurredAt(event.occurredAt());
                entity.setObservedAt(event.observedAt());
                entity.setSource(toJson(event.source()));
                entity.setData(toJson(event.data()));
                entity.setRawEvent(toJson(event.data()));
                entity.setStatus("PENDING");
                entity.setRetryCount(0);
                entity = eventRepository.save(entity);
                eventIds.add(entity.getId());
            }

            eventPublisher.publishEvent(new TrassirEventsAcceptedEvent(
                    raw.getId(),
                    envelope.server().serverId(),
                    envelope.batchId(),
                    receivedAt,
                    eventIds
            ));

            metrics.incrementIngest("accepted");
        } finally {
            metrics.stopIngest(sample);
        }
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "{}";
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }
}
