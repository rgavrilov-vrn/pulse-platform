package ru.agroeco.pulse.incident.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class DeleteIncidentMaterialService {
    private static final Logger log = LoggerFactory.getLogger(DeleteIncidentMaterialService.class);
    private final JdbcTemplate jdbc;
    private final Path root;

    public DeleteIncidentMaterialService(JdbcTemplate jdbc,
            @Value("${app.storage.local.root-directory:${INCIDENT_FILES_DIRECTORY:./data/incident-files}}") String directory) throws IOException {
        this.jdbc = jdbc;
        Files.createDirectories(Path.of(directory).toAbsolutePath().normalize());
        this.root = Path.of(directory).toRealPath();
    }

    @Transactional
    public void delete(UUID incidentId, UUID materialId, UUID actorId) {
        var statuses = jdbc.queryForList("SELECT status FROM incident.incidents WHERE id=? FOR UPDATE", String.class, incidentId);
        if (statuses.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found");
        if (!"DRAFT".equals(statuses.getFirst()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Materials can only be removed from draft incidents");
        var assets = jdbc.query(
                "SELECT m.storage_provider, m.storage_key FROM incident.media_assets m JOIN incident.incident_materials im ON im.media_asset_id=m.id WHERE im.incident_id=? AND m.id=? FOR UPDATE OF m",
                (rs, row) -> new String[] { rs.getString("storage_provider"), rs.getString("storage_key") }, incidentId, materialId);
        if (assets.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material link not found");
        String[] asset = assets.getFirst();
        if (!"LOCAL".equals(asset[0]) || !materialId.toString().equals(asset[1]))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Material is not managed by local storage");
        jdbc.update("DELETE FROM incident.incident_materials WHERE incident_id=? AND media_asset_id=?", incidentId, materialId);
        Long references = jdbc.queryForObject("SELECT count(*) FROM incident.incident_materials WHERE media_asset_id=?", Long.class, materialId);
        boolean unused = references != null && references == 0;
        if (unused) jdbc.update("DELETE FROM incident.media_assets WHERE id=?", materialId);
        Path file = root.resolve(asset[1]);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                log.info("Material detached: incidentId={}, materialId={}, actorId={}, unused={}", incidentId, materialId, actorId, unused);
                if (unused) {
                    try { Files.deleteIfExists(file); }
                    catch (IOException exception) {
                        log.error("Material metadata deleted but disk cleanup failed: materialId={}, path={}", materialId, file, exception);
                    }
                }
            }
        });
    }
}
