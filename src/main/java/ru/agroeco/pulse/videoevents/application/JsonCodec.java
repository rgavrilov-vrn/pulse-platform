package ru.agroeco.pulse.videoevents.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared immutable-after-configuration mapper for persistence serialization.
 */
public final class JsonCodec {
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private JsonCodec() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize JSON payload", e);
        }
    }
}
