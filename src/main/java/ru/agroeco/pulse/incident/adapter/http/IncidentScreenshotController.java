package ru.agroeco.pulse.incident.adapter.http;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.agroeco.pulse.incident.application.IncidentMaterialService;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import ru.agroeco.pulse.trassir.application.TrassirScreenshotClient;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/incidents/{incidentId}/materials/trassir-screenshot")
public class IncidentScreenshotController {
    private final JdbcTemplate jdbc;
    private final TrassirScreenshotClient client;
    private final IncidentMaterialService materials;

    public record Availability(boolean canRequestTrassirScreenshot, String reason) {}
    private record Target(String objectId, String baseUrl, String timestamp, String fileName) {}

    @GetMapping
    @PreAuthorize("hasAuthority('incident:read')")
    public Availability availability(@PathVariable UUID incidentId) {
        try {
            target(incidentId);
            return new Availability(true, null);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 409) throw exception;
            return new Availability(false, exception.getReason());
        }
    }

    @PostMapping("/retry")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('incident:write')")
    public IncidentMaterialService.MaterialView retry(@PathVariable UUID incidentId,
            @AuthenticationPrincipal AppPrincipal principal) {
        Target target = target(incidentId);
        byte[] bytes;
        try {
            bytes = client.download(target.baseUrl(), target.objectId(), target.timestamp());
        } catch (Exception exception) {
            boolean timeout = false;
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.net.http.HttpTimeoutException
                        || (cause.getMessage() != null && cause.getMessage().toLowerCase(Locale.ROOT).contains("timeout"))) {
                    timeout = true;
                    break;
                }
            }
            throw new ResponseStatusException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY,
                    timeout ? "Истекло время ожидания скриншота Trassir" : "Не удалось получить скриншот Trassir");
        }
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 255) == 255
                && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
        boolean png = bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80
                && bytes[2] == 78 && bytes[3] == 71 && bytes[4] == 13 && bytes[5] == 10
                && bytes[6] == 26 && bytes[7] == 10;
        if (!jpeg && !png) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Trassir вернул ответ, не похожий на JPEG или PNG");
        String extension = jpeg ? ".jpg" : ".png";
        String name = target.fileName();
        if (name == null || name.isBlank()) name = "trassir-" + incidentId;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        if (name.isBlank()) name = "trassir-" + incidentId;
        if (name.length() > 240) name = name.substring(0, 240);
        return materials.uploadSystemMaterial(incidentId, name + extension,
                jpeg ? "image/jpeg" : "image/png", bytes, principal.id());
    }

    private Target target(UUID id) {
        var incidents = jdbc.query("SELECT source, metadata->>'video_timestamp' AS video_timestamp, metadata->>'image_file' AS image_file FROM incident.incidents WHERE id=?",
                (rs, row) -> new String[] {rs.getString("source"), rs.getString("video_timestamp"), rs.getString("image_file")}, id);
        if (incidents.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Инцидент не найден");
        String[] incident = incidents.getFirst();
        if (!"manual".equals(incident[0]) || incident[1] == null || !incident[1].matches("[0-9]{8}_[0-9]{6}"))
            throw conflict("Нет исходного video_timestamp ручного инцидента Trassir");
        List<Target> targets = jdbc.query("""
                SELECT CASE WHEN r.entity_type='channel' THEN ch.external_id ELSE c.external_id END AS external_id,
                       s.base_url, s.host
                FROM incident.context_refs r
                LEFT JOIN videoserver.channels ch ON r.entity_type='channel' AND ch.id=r.entity_id
                JOIN videoserver.cameras c ON c.id=CASE WHEN r.entity_type='channel' THEN ch.camera_id ELSE r.entity_id END
                JOIN videoserver.video_servers s ON s.id=c.video_server_id
                WHERE r.incident_id=? AND r.domain='videoserver'
                  AND r.entity_type IN ('camera','channel') AND s.source='trassir'
                ORDER BY r.is_primary DESC, r.entity_type, r.entity_id
                """, (rs, row) -> {
                    String url = rs.getString("base_url");
                    if (url == null || url.isBlank()) url = rs.getString("host");
                    if (url != null && !url.isBlank()) {
                        url = url.trim();
                        if (!url.startsWith("https://") && !url.startsWith("http://")) url = "https://" + url;
                    }
                    return new Target(rs.getString("external_id"), url, incident[1], incident[2]);
                }, id);
        if (targets.isEmpty()) throw conflict("Нет привязки к камере или каналу Trassir");
        Target target = targets.getFirst();
        if (target.objectId() == null || target.objectId().isBlank()) throw conflict("Не задан внешний ID камеры или канала");
        if (target.baseUrl() == null || target.baseUrl().isBlank()) throw conflict("Не задан адрес видеосервера");
        return target;
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
