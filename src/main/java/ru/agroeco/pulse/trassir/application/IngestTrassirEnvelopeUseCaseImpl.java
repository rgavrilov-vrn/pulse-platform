package ru.agroeco.pulse.trassir.application;

import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirEventRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirIngestRawRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
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
            UUID rawId = UUID.randomUUID();

            // FIX #27: атомарная вставка — race на existsByBatchId устранён.
            int inserted = rawRepository.insertIfAbsent(
                    rawId,
                    envelope.batchId(),
                    receivedAt,
                    envelope.sentAt(),
                    envelope.producer() != null ? envelope.producer().name() : null,
                    envelope.producer() != null ? envelope.producer().version() : null,
                    envelope.server().serverId(),
                    rawBody);

            if (inserted == 0) {
                metrics.incrementIngest("duplicate");
                log.info("Duplicate TRASSIR batch ignored: batchId={}, serverId={}",
                        envelope.batchId(), envelope.server().serverId());
                return;
            }

            List<UUID> acceptedEventIds = new ArrayList<>(envelope.events().size());
            int index = 0;
            for (ParsedTrassirEnvelope.Event event : envelope.events()) {
                UUID eventId = UUID.randomUUID();
                // FIX #27: атомарная вставка; event_id дубликат — skip.
                int row = eventRepository.insertIfAbsent(
                        eventId, rawId, envelope.server().serverId(), index++,
                        event.eventId(), event.eventType(), event.eventVersion(),
                        event.occurredAt(), event.observedAt(),
                        // FIX #83: raw_event = полный event, а не только data.
                        toJson(event.source()), toJson(event.data()), toJson(event));
                if (row == 1) {
                    acceptedEventIds.add(eventId);
                } else {
                    metrics.incrementIngest("duplicate_event");
                    log.debug("Duplicate TRASSIR event ignored: eventId={}", event.eventId());
                }
            }

            if (!acceptedEventIds.isEmpty()) {
                eventPublisher.publishEvent(new TrassirEventsAcceptedEvent(
                        rawId, envelope.server().serverId(),
                        envelope.batchId(), receivedAt, acceptedEventIds));
            }

            metrics.incrementIngest("accepted");
        } catch (RuntimeException e) {
            // FIX #78: rejected-метрика — раньше ingest-ошибки не попадали в счётчик.
            metrics.incrementIngest("rejected");
            throw e;
        } finally {
            metrics.stopIngest(sample);
        }
    }

    private String toJson(Object node) {
        if (node == null) return "{}";
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Cannot serialize TRASSIR payload", e);
            return "{}";
        }
    }
}