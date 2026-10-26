package ru.agroeco.pulse.goodwan.api;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoodWanAuthenticationClient {

    private final RestClient restClient;

    public GoodWanAuthenticationClient(RestClient goodWanRestClient) {
        this.restClient = goodWanRestClient;
    }

    public GoodWanAuthenticationStatus checkAuthentication() {
        try {
            restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/data/history")
                    .queryParam("deviceid", 0)
                    .queryParam("fromId", 0)
                    .queryParam("count", 1)
                    .build())
                .retrieve()
                .toBodilessEntity();

            return new GoodWanAuthenticationStatus(
                true,
                200,
                "GoodWAN authentication successful"
            );
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();

            return new GoodWanAuthenticationStatus(
                status == 200,
                status,
                "GoodWAN authentication failed"
            );
        } catch (Exception e) {
            return new GoodWanAuthenticationStatus(
                false,
                0,
                "GoodWAN is unavailable: " + e.getMessage()
            );
        }
    }
}
