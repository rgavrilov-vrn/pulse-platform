package ru.agroeco.pulse.trassir.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.trassir.adapter.http.dto.TrassirManualIncidentRequest;
import ru.agroeco.pulse.trassir.application.TrassirManualIncidentService;

@RestController
@RequestMapping("/api/v1/trassir/incidents")
@RequiredArgsConstructor
@Tag(name = "TRASSIR · Incidents", description = "Ручное создание инцидентов от TRASSIR")
public class TrassirManualIncidentController {

    private final TrassirManualIncidentService service;

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('trassir:ingest')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Принять ручной инцидент от TRASSIR (flat JSON)")
    public void receiveManualIncident(@RequestBody TrassirManualIncidentRequest request) {
        service.handleManualIncident(request);
    }
}