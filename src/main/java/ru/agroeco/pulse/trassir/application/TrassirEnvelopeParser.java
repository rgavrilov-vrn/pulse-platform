package ru.agroeco.pulse.trassir.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component("trassirModuleEnvelopeParser")
public class TrassirEnvelopeParser {

    private static final String CONTRACT = "trassir.telemetry";
    private static final String CONTRACT_VERSION = "1.0";

    private final ObjectMapper objectMapper;
    private final int maxBodyBytes;

    public TrassirEnvelopeParser(
            ObjectMapper objectMapper,
            // FIX #82: лимит размера body. application/json не ограничен
            // spring.servlet.multipart.max-* — клиент мог прислать 100 МБ.
            @Value("${pulse.trassir.ingest.max-body-bytes:10485760}") int maxBodyBytes) {
        this.objectMapper = objectMapper;
        this.maxBodyBytes = Math.max(1024, maxBodyBytes);
    }

    public ParsedTrassirEnvelope parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw badRequest("EMPTY_TRASSIR_BODY");
        }
        // FIX #82
        int bytes = rawBody.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > maxBodyBytes) {
            log.warn("TRASSIR ingest body too large: {} bytes > {} limit", bytes, maxBodyBytes);
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "TRASSIR_BODY_TOO_LARGE");
        }
        log.debug("TRASSIR ingest body prefix: {}",
                rawBody.substring(0, Math.min(rawBody.length(), 500)));
        try {
            ParsedTrassirEnvelope envelope =
                    objectMapper.readValue(rawBody, ParsedTrassirEnvelope.class);
            validate(envelope);
            return envelope;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // FIX #84: malformed JSON — ожидаемое событие, стек в DEBUG.
            log.debug("Failed to parse TRASSIR envelope: {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw badRequest("INVALID_TRASSIR_ENVELOPE");
        }
    }

    private void validate(ParsedTrassirEnvelope envelope) {
        if (!CONTRACT.equals(envelope.contract())) {
            throw badRequest("UNSUPPORTED_TRASSIR_CONTRACT");
        }
        if (!CONTRACT_VERSION.equals(envelope.contractVersion())) {
            throw badRequest("UNSUPPORTED_TRASSIR_CONTRACT_VERSION");
        }
        if (envelope.batchId() == null) {
            throw badRequest("MISSING_TRASSIR_BATCH_ID");
        }
        if (envelope.server() == null
                || envelope.server().serverId() == null
                || envelope.server().serverId().isBlank()) {
            throw badRequest("MISSING_TRASSIR_SERVER_ID");
        }
        // FIX #79: producer обязателен по контракту trassir.telemetry/1.0;
        // producer.name — обязателен. Ранее validate() это пропускал,
        // и падало на NOT NULL constraint ingest_raw.producer_name (409).
        if (envelope.producer() == null
                || envelope.producer().name() == null
                || envelope.producer().name().isBlank()) {
            throw badRequest("MISSING_TRASSIR_PRODUCER");
        }
        if (envelope.events() == null) {
            throw badRequest("MISSING_TRASSIR_EVENTS");
        }
    }

    private ResponseStatusException badRequest(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }
}