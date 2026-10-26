package ru.agroeco.pulse.goodwan.raw;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class RawDataHistoryMapper {

    private static final String ENDPOINT = "/data/history";

    private final ObjectMapper objectMapper;

    public RawDataHistoryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RawDataHistoryEntity toEntity(
        String deviceId,
        RawDataHistoryItem item,
        String requestParametersJson,
        Instant requestedAt,
        Instant receivedAt
    ) {
        try {
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
        } catch (JsonProcessingException e) {
            throw new GoodWanRawMappingException(
                "Cannot serialize GoodWAN data history item",
                e
            );
        }
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
