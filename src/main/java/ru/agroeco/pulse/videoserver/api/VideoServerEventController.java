package ru.agroeco.pulse.videoserver.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.agroeco.pulse.videoserver.application.IngestRawVideoEventUseCase;

@RestController
@RequestMapping("/api/v1/videoserver/events")
@Tag(name = "Video Servers", description = "Управление видеосерверами (VMS)")
public class VideoServerEventController {

    private final IngestRawVideoEventUseCase ingestRawVideoEventUseCase;

    public VideoServerEventController(IngestRawVideoEventUseCase ingestRawVideoEventUseCase) {
        this.ingestRawVideoEventUseCase = ingestRawVideoEventUseCase;
    }

    @PostMapping("/raw")
    @ResponseStatus(HttpStatus.CREATED)
    public VideoEventNormalized ingestRawEvent(@Valid @RequestBody RawVideoEventRequest request) {
        return ingestRawVideoEventUseCase.handle(request);
    }
}
