package ru.agroeco.pulse.platform.trassir.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Temporary adapter while callers migrate to the trassir module. */
@Component
public class TrassirEnvelopeParser {

    private final ru.agroeco.pulse.trassir.application.TrassirEnvelopeParser delegate;

    @Autowired
    public TrassirEnvelopeParser(ru.agroeco.pulse.trassir.application.TrassirEnvelopeParser delegate) {
        this.delegate = delegate;
    }

    public TrassirEnvelopeParser(ObjectMapper objectMapper) {
        this(new ru.agroeco.pulse.trassir.application.TrassirEnvelopeParser(objectMapper));
    }

    public ParsedTrassirEnvelope parse(String rawBody) {
        var envelope = delegate.parse(rawBody);
        var producer = envelope.producer();
        return new ParsedTrassirEnvelope(
                envelope.contract(), envelope.contractVersion(), envelope.batchId(), envelope.sentAt(),
                producer == null ? null : new ParsedTrassirEnvelope.Producer(producer.name(), producer.version()),
                new ParsedTrassirEnvelope.Server(envelope.server().serverId()),
                envelope.events().stream().map(event -> event == null ? null : new ParsedTrassirEnvelope.Event(
                        event.eventId(), event.eventType(), event.eventVersion(), event.occurredAt(),
                        event.observedAt(), event.source(), event.data())).toList());
    }
}
