package ru.agroeco.pulse.incident.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IncidentMaterialService {
    private static final Logger log = LoggerFactory.getLogger(IncidentMaterialService.class);
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"), Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"), Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"), Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final Path root;
    private final long maxBytes;

    public IncidentMaterialService(JdbcTemplate jdbc, PlatformTransactionManager manager,
            @Value("${app.storage.local.root-directory:${INCIDENT_FILES_DIRECTORY:./data/incident-files}}") String directory,
            @Value("${app.storage.max-file-size:50MB}") String maximum) throws IOException {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.maxBytes = DataSize.parse(maximum).toBytes();
        if (maxBytes <= 0) throw new IllegalArgumentException("Storage max-file-size must be positive");
        Files.createDirectories(Path.of(directory).toAbsolutePath().normalize());
        this.root = Path.of(directory).toRealPath();
        Path probe = Files.createTempFile(root, "probe-", ".tmp");
        Files.delete(probe);
    }

    public record MaterialView(UUID id, String fileName, String mimeType, long sizeBytes,
            String materialType, String role, Instant uploadedAt, String downloadUrl) {}
    public record Download(MaterialView material, Resource resource) {}

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private void requireIncident(UUID id, boolean lock, boolean draft) {
        List<String> statuses = jdbc.queryForList(
                "SELECT status FROM incident.incidents WHERE id = ?" + (lock ? " FOR UPDATE" : ""), String.class, id);
        if (statuses.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Incident not found");
        if (draft && !"DRAFT".equals(statuses.getFirst()))
            throw error(HttpStatus.CONFLICT, "Files can only be attached to a draft incident");
    }

    private MaterialView view(ResultSet rs, UUID incidentId) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new MaterialView(id, rs.getString("file_name"), rs.getString("mime_type"),
                rs.getLong("size_bytes"), rs.getString("material_type"), rs.getString("role"),
                rs.getTimestamp("uploaded_at").toInstant(),
                "/api/v1/incidents/" + incidentId + "/materials/" + id + "/download");
    }

    public List<MaterialView> list(UUID incidentId) {
        requireIncident(incidentId, false, false);
        return jdbc.query("SELECT m.*, im.role FROM incident.media_assets m JOIN incident.incident_materials im ON im.media_asset_id=m.id WHERE im.incident_id=? ORDER BY im.sort_order, m.uploaded_at, m.id",
                (rs, row) -> view(rs, incidentId), incidentId);
    }

    public MaterialView upload(UUID incidentId, MultipartFile file, UUID actorId) throws IOException {
        requireIncident(incidentId, false, true);
        if (file.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "Empty file");
        if (file.getSize() > maxBytes) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "File size limit exceeded");
        String supplied = file.getOriginalFilename();
        String name = supplied == null ? "file" : supplied.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
        if (name.isBlank() || name.length() > 255) throw error(HttpStatus.BAD_REQUEST, "Invalid file name");
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String mime = TYPES.get(extension);
        if (mime == null) throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File extension is not allowed");
        String kind = mime.startsWith("image/") ? "IMAGE" : mime.startsWith("video/") ? "VIDEO" : "DOCUMENT";
        UUID assetId = UUID.randomUUID();
        String key = assetId.toString();
        Path target = root.resolve(key);
        Path temporary = Files.createTempFile(root, "upload-", ".tmp");
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) {
            Files.deleteIfExists(temporary);
            throw new IllegalStateException(exception);
        }
        long length = 0;
        try {
            try (InputStream input = file.getInputStream(); OutputStream output = Files.newOutputStream(temporary)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    length += count;
                    if (length > maxBytes) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "File size limit exceeded");
                    digest.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
            }
            if (length == 0) throw error(HttpStatus.BAD_REQUEST, "Empty file");
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, target); }
        } catch (IOException | RuntimeException exception) {
            cleanup(temporary);
            throw exception;
        }
        String finalName = name;
        long bytes = length;
        String sha = HexFormat.of().formatHex(digest.digest());
        Instant uploadedAt = Instant.now();
        AtomicBoolean registered = new AtomicBoolean(false);
        try {
            return transactions.execute(transaction -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) cleanup(target);
                    }
                });
                registered.set(true);
                requireIncident(incidentId, true, true);
                jdbc.update("INSERT INTO incident.media_assets (id,storage_provider,storage_key,file_name,mime_type,file_extension,size_bytes,sha256,material_type,uploaded_at,uploaded_by) VALUES (?,'LOCAL',?,?,?,?,?,?,?, ?,?)",
                        assetId, key, finalName, mime, extension, bytes, sha, kind, java.sql.Timestamp.from(uploadedAt), actorId);
                jdbc.update("INSERT INTO incident.incident_materials (incident_id,media_asset_id,role,created_by) VALUES (?,?,'ADDITIONAL_EVIDENCE',?)", incidentId, assetId, actorId);
                log.info("Material attached: incidentId={}, materialId={}, actorId={}, bytes={}, sha256={}", incidentId, assetId, actorId, bytes, sha);
                return new MaterialView(assetId, finalName, mime, bytes, kind, "ADDITIONAL_EVIDENCE", uploadedAt,
                        "/api/v1/incidents/" + incidentId + "/materials/" + assetId + "/download");
            });
        } catch (RuntimeException exception) {
            if (!registered.get()) cleanup(target);
            throw exception;
        }
    }

    private void cleanup(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException exception) { log.error("Storage cleanup failed for {}", path, exception); }
    }

    public Download download(UUID incidentId, UUID materialId) throws IOException {
        requireIncident(incidentId, false, false);
        var rows = jdbc.query("SELECT m.*, im.role FROM incident.media_assets m JOIN incident.incident_materials im ON im.media_asset_id=m.id WHERE im.incident_id=? AND m.id=?",
                (rs, row) -> new Object[] { view(rs, incidentId), rs.getString("storage_provider"), rs.getString("storage_key") }, incidentId, materialId);
        if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Material not found");
        Object[] row = rows.getFirst();
        if (!"LOCAL".equals(row[1])) throw error(HttpStatus.CONFLICT, "Material is not in local storage");
        String key = (String) row[2];
        if (!materialId.toString().equals(key)) throw error(HttpStatus.CONFLICT, "Invalid storage key");
        Path path = root.resolve(key);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw error(HttpStatus.NOT_FOUND, "Stored file is unavailable");
        var channel = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
        return new Download((MaterialView) row[0], new InputStreamResource(Channels.newInputStream(channel)));
    }
}
