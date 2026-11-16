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
    private final String password;
    private final Duration readTimeout;

    public TrassirScreenshotClient(
            @Value("${trassir.screenshot.password:Script}") String password,
            @Value("${trassir.screenshot.connect-timeout:PT3S}") Duration connectTimeout,
            @Value("${trassir.screenshot.read-timeout:PT10S}") Duration readTimeout) {
        this.password = password;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    public byte[] download(String videoServerBaseUrl, String objectId, String videoTimestamp) {
        if (videoServerBaseUrl == null || videoServerBaseUrl.isBlank())
            throw new IllegalArgumentException("Video server base URL is required");
        if (objectId == null || objectId.isBlank() || videoTimestamp == null || videoTimestamp.isBlank())
            throw new IllegalArgumentException("Screenshot objectId and videoTimestamp are required");
        URI base = URI.create(videoServerBaseUrl.trim());
        if (base.getHost() == null || base.getRawUserInfo() != null
                || !("https".equalsIgnoreCase(base.getScheme()) || "http".equalsIgnoreCase(base.getScheme())))
            throw new IllegalArgumentException("Invalid video server base URL");
        String host = base.getHost();
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        int port = base.getPort() == -1 ? 8080 : base.getPort();
        String path = base.getRawPath() == null ? "" : base.getRawPath().replaceAll("/+$", "");
        String prefix = base.getScheme() + "://" + host + ":" + port + path
                + "/screenshot/" + encode(objectId) + "?timestamp=" + encode(videoTimestamp);
        String logUrl = prefix + "&password=***";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(prefix + "&password=" + encode(password)))
                .timeout(readTimeout).GET().build();
        log.info("Trassir screenshot request: method=GET, url={}", logUrl);
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            log.info("Trassir screenshot response: url={}, status={}", logUrl, response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Trassir screenshot HTTP status: " + response.statusCode());
            byte[] body = response.body();
            if (body == null || body.length == 0)
                throw new IllegalStateException("Trassir returned an empty screenshot");
            return body;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Trassir screenshot interrupted: url=" + logUrl);
        } catch (Exception exception) {
            // Do not propagate exceptions that may contain the credential-bearing URI.
            throw new IllegalStateException("Failed to download Trassir screenshot: url="
                    + logUrl + ", failure=" + exception.getClass().getSimpleName());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
