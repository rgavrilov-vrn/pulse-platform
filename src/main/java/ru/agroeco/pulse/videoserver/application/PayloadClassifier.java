package ru.agroeco.pulse.videoserver.application;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
            VideoEventType type = mapSubtype(node.path("type").asString(""));
            EventSeverity severity = mapSeverity(node.path("severity").asString(""));
            return new Classification(type, severity);
        } catch (JacksonException ex) {
            throw new BusinessRuleException("INVALID_RAW_PAYLOAD: " + ex.getOriginalMessage());
        }
    }

    public static VideoEventType mapSubtype(String subtype) {
        if (subtype == null || subtype.isBlank()) return VideoEventType.UNKNOWN;
        return switch (subtype.toLowerCase().trim()) {
            case "motion", "motion_detected"                          -> VideoEventType.MOTION_DETECTED;
            case "offline", "camera_offline", "connection lost"       -> VideoEventType.CAMERA_OFFLINE;
            case "tamper", "tampering", "camera_tampering"            -> VideoEventType.CAMERA_TAMPERING;
            // Событие "Connection Established" не имеет своего типа в enum, используем дефолтный
            case "connection established"                             -> VideoEventType.VIDEO_EVENT_RECEIVED;
            default -> VideoEventType.VIDEO_EVENT_RECEIVED;
        };
    }

    public static EventSeverity mapSeverity(String value) {
        if (value == null || value.isBlank()) return EventSeverity.INFO;
        return switch (value.toLowerCase().trim()) {
            case "critical", "tamper" -> EventSeverity.CRITICAL;
            case "warning", "motion", "connection lost" -> EventSeverity.WARNING;
            default -> EventSeverity.INFO;
        };
    }

    public record Classification(VideoEventType type, EventSeverity severity) {
    }
}