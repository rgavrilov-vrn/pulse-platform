package ru.agroeco.pulse.trassir.adapter.http;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.trassir.application.IngestResult;
import ru.agroeco.pulse.trassir.application.IngestTrassirEnvelopeUseCase;
import ru.agroeco.pulse.trassir.application.TrassirIngestLogService;

@Slf4j
@RestController
@RequestMapping("/api/v1/trassir")
@RequiredArgsConstructor
public class TrassirIngestController {

    private final IngestTrassirEnvelopeUseCase ingestTrassirEnvelopeUseCase;
    private final TrassirIngestLogService ingestLogService;

    @Value("${pulse.trassir.ingest.max-body-bytes:10485760}")
    private int maxBodyBytes;

    @PostMapping(path = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> ingest(@RequestBody String rawBody,
                                       HttpServletRequest request) {
        long start = System.nanoTime();
        IngestResult result = null;
        Throwable error = null;

        try {
            long declared = request.getContentLengthLong();
            if (declared > maxBodyBytes) {
                error = new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                        "TRASSIR_BODY_TOO_LARGE");
                throw (ResponseStatusException) error;
            }

            result = ingestTrassirEnvelopeUseCase.handle(rawBody);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (error == null) error = e;
            throw e;
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            ingestLogService.record(request, rawBody, result, error, durationMs);
        }
    }
}