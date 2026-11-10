package ru.agroeco.pulse.platform.trassir.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class TrassirEnvelopeParser {

    private static final String CONTRACT = "trassir.telemetry";
    private static final String CONTRACT_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public TrassirEnvelopeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedTrassirEnvelope parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw badRequest("EMPTY_TRASSIR_BODY");
        }

        log.debug("TRASSIR ingest body prefix: {}",
                rawBody.substring(0, Math.min(rawBody.length(), 500)));

        try {
            ParsedTrassirEnvelope envelope = objectMapper.readValue(
                    rawBody,
                    ParsedTrassirEnvelope.class
            );
            validate(envelope);
            return envelope;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse TRASSIR envelope: {}: {}",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
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

        if (envelope.events() == null) {
            throw badRequest("MISSING_TRASSIR_EVENTS");
        }
    }

    private ResponseStatusException badRequest(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }
}
