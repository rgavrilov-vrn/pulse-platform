package ru.agroeco.pulse.platform.trassir.application;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParsedTrassirEnvelope(
        String contract,
        @JsonAlias("contract_version") String contractVersion,
        UUID batchId,
        Instant sentAt,
        Producer producer,
        Server server,
        List<Event> events
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Producer(
            String name,
            String version
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Server(
            @JsonAlias("server_id") String serverId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
            @JsonAlias("event_id") UUID eventId,
            @JsonAlias("event_type") String eventType,
            @JsonAlias("event_version") String eventVersion,
            Instant occurredAt,
            Instant observedAt,
            String source,
            String data
    ) {
    }
}
