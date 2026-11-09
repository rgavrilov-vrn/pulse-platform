package ru.agroeco.pulse.platform.trassir.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.agroeco.pulse.platform.trassir.application.IngestTrassirEnvelopeUseCase;

@RestController
@RequestMapping("/api/v1/trassir")
@RequiredArgsConstructor
public class TrassirIngestController {

    private final IngestTrassirEnvelopeUseCase ingestTrassirEnvelopeUseCase;

    @PostMapping(path = "/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> ingest(@RequestBody String rawBody) {
        ingestTrassirEnvelopeUseCase.handle(rawBody);
        return ResponseEntity.ok().build();
    }
}
