package ru.agroeco.pulse.trassir.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
public class TrassirScreenshotClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String password;
    private final Duration readTimeout;

    public TrassirScreenshotClient(
            @Value("${trassir.screenshot.base-url:https://10.81.0.82:8080}") String baseUrl,
            @Value("${trassir.screenshot.password:Script}") String password,
            @Value("${trassir.screenshot.connect-timeout:PT3S}") Duration connectTimeout,
            @Value("${trassir.screenshot.read-timeout:PT10S}") Duration readTimeout
    ) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.password = password;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    public byte[] download(String objectId, String videoTimestamp) {
        String uri = baseUrl + "/screenshot/"
                + URLEncoder.encode(objectId, StandardCharsets.UTF_8)
                + "?timestamp=" + URLEncoder.encode(videoTimestamp, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(readTimeout)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Trassir screenshot request failed: status=%d, objectId=%s, timestamp=%s"
                                .formatted(response.statusCode(), objectId, videoTimestamp)
                );
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new IllegalStateException(
                        "Trassir returned an empty screenshot: objectId=%s, timestamp=%s"
                                .formatted(objectId, videoTimestamp)
                );
            }

            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Trassir screenshot download was interrupted: objectId=%s".formatted(objectId),
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to download Trassir screenshot: objectId=%s, timestamp=%s"
                            .formatted(objectId, videoTimestamp),
                    e
            );
        }
    }
}
