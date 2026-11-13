package ru.agroeco.pulse.trassir.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ParsedTrassirEnvelope(
        String contract,
        @JsonProperty("contract_version") String contractVersion,
        @JsonProperty("batch_id") UUID batchId,
        @JsonProperty("sent_at") Instant sentAt,
        @JsonProperty("producer") Producer producer,
        @JsonProperty("server") Server server,
        @JsonProperty("events") List<Event> events
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Producer(String name, String version) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Server(@JsonProperty("server_id") UUID serverId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
            @JsonProperty("event_id") UUID eventId,
            @JsonProperty("event_type") String eventType,
            @JsonProperty("event_version") String eventVersion,
            @JsonProperty("occurred_at") Instant occurredAt,
            @JsonProperty("observed_at") Instant observedAt,
            @JsonProperty("source") JsonNode source,
            @JsonProperty("data") JsonNode data
    ) {}
}