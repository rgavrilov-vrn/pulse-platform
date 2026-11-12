package ru.agroeco.pulse.platform.adapter.http.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(Instant timestamp, int status, String code, String message, List<String> details, String path, String correlationId) {
}
