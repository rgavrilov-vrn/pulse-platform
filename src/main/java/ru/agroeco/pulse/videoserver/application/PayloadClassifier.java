package ru.agroeco.pulse.videoserver.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.videoserver.api.EventSeverity;
import ru.agroeco.pulse.videoserver.api.VideoEventType;

@Component
@RequiredArgsConstructor
public class PayloadClassifier {

    private final ObjectMapper mapper;

    public Classification classify(String rawPayload) {
        try {
            JsonNode node = mapper.readTree(rawPayload);
            VideoEventType type = mapType(node.path("type").asText(""));
            EventSeverity severity = mapSeverity(node.path("severity").asText(""));
            return new Classification(type, severity);
        } catch (JsonProcessingException ex) {
            // защита: до этого места валидация @ValidJson не должна была пропустить
            throw new BusinessRuleException("INVALID_RAW_PAYLOAD: " + ex.getOriginalMessage());
        }
    }

    private VideoEventType mapType(String type) {
        if (type == null || type.isBlank()) return VideoEventType.UNKNOWN;
        return switch (type.toLowerCase().trim()) {
            case "motion", "motion_detected" -> VideoEventType.MOTION_DETECTED;
            case "offline", "camera_offline" -> VideoEventType.CAMERA_OFFLINE;
            case "tamper", "tampering", "camera_tampering" -> VideoEventType.CAMERA_TAMPERING;
            default -> VideoEventType.VIDEO_EVENT_RECEIVED;
        };
    }

    private EventSeverity mapSeverity(String value) {
        if (value == null || value.isBlank()) return EventSeverity.INFO;
        return switch (value.toLowerCase().trim()) {
            case "critical", "tamper" -> EventSeverity.CRITICAL;
            case "warning", "motion" -> EventSeverity.WARNING;
            default -> EventSeverity.INFO;
        };
    }

    public record Classification(VideoEventType type, EventSeverity severity) {
    }
}