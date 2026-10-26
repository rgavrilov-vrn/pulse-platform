package ru.agroeco.pulse.goodwan.raw;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class RawDataHistoryMapper {

    private static final String ENDPOINT = "/data/history";

    public RawDataHistoryEntity toEntity(
            String deviceId,
            RawDataHistoryItem item,
            String requestParametersJson,
            Instant requestedAt,
            Instant receivedAt
    ) {
        String responseItemJson = item.responseItemJson();

        return RawDataHistoryEntity.create(
                deviceId,
                item.sourceRecordId(),
                ENDPOINT,
                requestParametersJson,
                responseItemJson,
                requestedAt,
                receivedAt,
                sha256(responseItemJson)
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}