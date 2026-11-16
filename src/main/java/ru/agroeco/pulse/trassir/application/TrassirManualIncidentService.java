package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.agroeco.pulse.trassir.adapter.http.dto.TrassirManualIncidentRequest;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirManualIncidentService {

    private final IngestTrassirEnvelopeUseCase ingestUseCase;
    private final ObjectMapper objectMapper;

    public void handleManualIncident(TrassirManualIncidentRequest req) {
        try {
            // Формируем стандартный конверт
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("contract", "trassir.telemetry");
            envelope.put("contract_version", "1.0");
            envelope.put("batch_id", UUID.randomUUID().toString());
            envelope.put("sent_at", Instant.now().toString());
            envelope.put("producer", Map.of("name", "manual-api", "version", "1.0"));
            // В вашем JSON нет server_id, берем дефолтный, либо нужно добавить его в JSON
            envelope.put("server", Map.of("server_id", "11111111-1111-1111-1111-111111111111"));
            envelope.put("events", List.of(req));

            String rawJson = objectMapper.writeValueAsString(envelope);

            // Отправляем в стандартный пайплайн
            ingestUseCase.handle(rawJson);
            log.info("Manual incident successfully wrapped and sent to ingest: eventId={}", req.eventId());
        } catch (Exception e) {
            log.error("Failed to process manual incident", e);
            throw new IllegalStateException("Failed to process manual incident", e);
        }
    }
}