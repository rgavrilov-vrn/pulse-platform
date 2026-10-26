package ru.agroeco.pulse.goodwan.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.agroeco.pulse.goodwan.raw.RawDataHistoryItem;

@Component
public class GoodWanDataHistoryClient {

    private final RestClient restClient;

    public GoodWanDataHistoryClient(RestClient goodWanRestClient) {
        this.restClient = goodWanRestClient;
    }

    public List<RawDataHistoryItem> fetchHistory(
        String deviceId,
        long fromId,
        int count
    ) {
        JsonNode response = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/data/history")
                .queryParam("deviceid", deviceId)
                .queryParam("fromId", fromId)
                .queryParam("count", count)
                .build())
            .retrieve()
            .body(JsonNode.class);

        if (response == null || !response.isArray()) {
            throw new GoodWanApiException(
                "GoodWAN /data/history response is not a JSON array"
            );
        }

        List<RawDataHistoryItem> items = new ArrayList<>();

        for (JsonNode item : response) {
            items.add(new RawDataHistoryItem(item));
        }

        return items;
    }
}
