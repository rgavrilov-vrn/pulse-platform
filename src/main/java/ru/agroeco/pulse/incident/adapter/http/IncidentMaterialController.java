package ru.agroeco.pulse.incident.adapter.http;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.agroeco.pulse.incident.application.IncidentMaterialService;
import ru.agroeco.pulse.incident.application.IncidentMaterialService.MaterialView;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/materials")
@RequiredArgsConstructor
public class IncidentMaterialController {
    private final IncidentMaterialService materials;

    @GetMapping
    @PreAuthorize("hasAuthority('incident:read')")
    public List<MaterialView> list(@PathVariable UUID incidentId) {
        return materials.list(incidentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('incident:write')")
    public MaterialView upload(@PathVariable UUID incidentId, @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppPrincipal principal) throws IOException {
        return materials.upload(incidentId, file, principal.id());
    }

    @GetMapping("/{materialId}/download")
    @PreAuthorize("hasAuthority('incident:read')")
    public ResponseEntity<Resource> download(@PathVariable UUID incidentId, @PathVariable UUID materialId) throws IOException {
        var download = materials.download(incidentId, materialId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.material().mimeType()))
                .contentLength(download.material().sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.material().fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(download.resource());
    }
}
