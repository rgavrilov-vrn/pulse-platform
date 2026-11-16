package ru.agroeco.pulse.trassir.adapter.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record TrassirManualIncidentRequest(
        @JsonProperty("event_type") String eventType,
        @JsonProperty("event_id") UUID eventId,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("observed_at") Instant observedAt,
        @JsonProperty("event_version") String eventVersion,
        @JsonProperty("source") Source source,
        @JsonProperty("data") Data data
) {
    public record Source(
            @JsonProperty("object_name") String objectName,
            @JsonProperty("object_class") String objectClass,
            @JsonProperty("object_id") String objectId
    ) {}

    public record Data(
            @JsonProperty("comment") String comment,
            @JsonProperty("reason") String reason,
            @JsonProperty("video_timestamp") String videoTimestamp,
            @JsonProperty("image_file") String imageFile
    ) {}
}