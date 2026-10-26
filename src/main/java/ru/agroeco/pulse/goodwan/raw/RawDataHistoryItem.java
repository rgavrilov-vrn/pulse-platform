package ru.agroeco.pulse.goodwan.raw;

import com.fasterxml.jackson.databind.JsonNode;

public record RawDataHistoryItem(
    JsonNode rawItem
) {

    public RawDataHistoryItem {
        if (rawItem == null || !rawItem.isObject()) {
            throw new IllegalArgumentException(
                "Raw data history item must be a JSON object"
            );
        }
    }

    public Long sourceRecordId() {
        JsonNode id = rawItem.path("id");

        return id.canConvertToLong()
            ? id.asLong()
            : null;
    }

    public String externalDeviceId() {
        JsonNode deviceId = rawItem.path("deviceId");

        if (deviceId.isMissingNode() || deviceId.isNull()) {
            return null;
        }

        return deviceId.asText();
    }

    public String responseItemJson() {
        return rawItem.toString();
    }
}
