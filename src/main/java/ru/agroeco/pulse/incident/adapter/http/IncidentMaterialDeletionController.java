package ru.agroeco.pulse.incident.adapter.http;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.incident.application.DeleteIncidentMaterialService;
import ru.agroeco.pulse.platform.security.AppPrincipal;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/materials")
@RequiredArgsConstructor
public class IncidentMaterialDeletionController {
    private final DeleteIncidentMaterialService deletion;

    @DeleteMapping("/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // FIX #64: раньше incident:write — оператор мог удалить вложение безвозвратно.
    // Удаление — отдельное право incident:delete (уже есть в auth.permissions).
    @PreAuthorize("hasAuthority('incident:delete')")
    public void delete(@PathVariable UUID incidentId, @PathVariable UUID materialId,
                       @AuthenticationPrincipal AppPrincipal principal) {
        deletion.delete(incidentId, materialId, principal.id());
    }
}