package ru.agroeco.pulse.trassir.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.trassir.application.IngestTrassirEnvelopeUseCase;

@RestController
@RequestMapping("/api/v1/trassir")
@RequiredArgsConstructor
public class TrassirIngestController {

    private final IngestTrassirEnvelopeUseCase ingestTrassirEnvelopeUseCase;

    @Value("${pulse.trassir.ingest.max-body-bytes:10485760}")
    private int maxBodyBytes;

    @PostMapping(path = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> ingest(@RequestBody String rawBody,
                                       HttpServletRequest request) {
        // FIX #82: ранний отказ по Content-Length до чтения/парсинга.
        // Точный размер (UTF-8) перепроверяется в TrassirEnvelopeParser —
        // Content-Length может отсутствовать (chunked) или врать.
        long declared = request.getContentLengthLong();
        if (declared > maxBodyBytes) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "TRASSIR_BODY_TOO_LARGE");
        }
        ingestTrassirEnvelopeUseCase.handle(rawBody);
        return ResponseEntity.ok().build();
    }
}