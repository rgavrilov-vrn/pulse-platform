package ru.agroeco.pulse.trassir.application;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirIngestLogRepository;
import ru.agroeco.pulse.trassir.application.dto.TrassirIngestLogView;
import ru.agroeco.pulse.trassir.domain.TrassirIngestLog;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirIngestLogService {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private final TrassirIngestLogRepository repository;

    @Value("${pulse.trassir.ingest.log-body-max-bytes:8192}")
    private int logBodyMaxBytes;

    // ============================================================ write

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, String rawBody,
                       IngestResult result, Throwable error, long durationMs) {
        try {
            TrassirIngestLog entry = buildLog(request, rawBody, result, error, durationMs);
            repository.save(entry);
        } catch (RuntimeException e) {
            log.error("Failed to persist ingest log", e);
        }
    }

    private TrassirIngestLog buildLog(HttpServletRequest req, String body,
                                      IngestResult result, Throwable error, long durationMs) {
        TrassirIngestLog entry = new TrassirIngestLog();

        entry.setReceivedAt(Instant.now().minusMillis(durationMs));
        entry.setDurationMs((int) durationMs);
        entry.setHttpMethod(req.getMethod());
        entry.setRequestUri(req.getRequestURI());

        // ✅ FIX: парсим IP в InetAddress
        entry.setRemoteAddr(parseInet(req.getRemoteAddr()));

        entry.setXForwardedFor(nullIfBlank(req.getHeader("X-Forwarded-For")));
        entry.setContentType(nullIfBlank(req.getContentType()));
        entry.setContentLength(req.getContentLengthLong() >= 0 ? req.getContentLengthLong() : null);
        entry.setUserAgent(nullIfBlank(req.getHeader("User-Agent")));
        entry.setAuthTokenPresent(req.getHeader(HEADER_AUTHORIZATION) != null);
        entry.setCorrelationId(CorrelationIds.current());

        if (body != null) {
            entry.setRequestBodyRead(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            entry.setRequestBodyBytes((long) bytes.length);
            entry.setRequestBodySha256(sha256(bytes));

            if (bytes.length <= logBodyMaxBytes) {
                entry.setRequestBodyPrefix(body);
                entry.setRequestBodyTruncated(false);
            } else {
                String prefix = new String(bytes, 0, logBodyMaxBytes, StandardCharsets.UTF_8);
                entry.setRequestBodyPrefix(prefix + "\n...[truncated at " + logBodyMaxBytes + " bytes]");
                entry.setRequestBodyTruncated(true);
            }
        } else {
            entry.setRequestBodyRead(false);
        }

        HttpStatusInfo response = deriveResponse(result, error);
        entry.setHttpStatus(response.status());
        entry.setResponseCode(response.code());
        entry.setResponseMessage(response.message());
        entry.setResponseBody(response.jsonBody());

        if (result != null) {
            entry.setBatchId(result.batchId());
            entry.setRawId(result.rawId());
            entry.setServerId(result.serverId());
            entry.setEventCount(result.eventCount());
        }

        return entry;
    }

    // ============================================================ read

    @Transactional(readOnly = true)
    public Page<TrassirIngestLogView> search(UUID batchId, UUID serverId, Integer httpStatus,
                                             String responseCode, Instant from, Instant to,
                                             Pageable pageable) {
        return repository.findAll(buildSpec(batchId, serverId, httpStatus, responseCode, from, to), pageable)
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public TrassirIngestLogView get(UUID id) {
        return repository.findById(id)
                .map(this::toView)
                .orElseThrow(() -> new ru.agroeco.pulse.platform.domain.exception.NotFoundException(
                        "Ingest log not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<TrassirIngestLogView> findByBatchId(UUID batchId) {
        return repository.findByBatchIdOrderByReceivedAtDesc(batchId).stream()
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> stats(Instant from, Instant to) {
        Instant fromEff = from != null ? from : Instant.EPOCH;
        Instant toEff = to != null ? to : Instant.now().plusSeconds(365L * 24 * 3600);

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        result.put("byHttpStatus",   toMap(repository.countByHttpStatus(fromEff, toEff)));
        result.put("byResponseCode", toMap(repository.countByResponseCode(fromEff, toEff)));
        return result;
    }

    @Transactional
    public int purgeBefore(Instant cutoff) {
        int n = repository.deleteOlderThan(cutoff);
        log.warn("Admin purge ingest_log before={}, deleted={}", cutoff, n);
        return n;
    }

    // ============================================================ spec

    private static Specification<TrassirIngestLog> buildSpec(UUID batchId, UUID serverId,
                                                             Integer httpStatus, String responseCode,
                                                             Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (batchId != null) p.add(cb.equal(root.get("batchId"), batchId));
            if (serverId != null) p.add(cb.equal(root.get("serverId"), serverId));
            if (httpStatus != null) p.add(cb.equal(root.get("httpStatus"), httpStatus));
            if (responseCode != null && !responseCode.isBlank())
                p.add(cb.equal(root.get("responseCode"), responseCode));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("receivedAt"), to));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    // ============================================================ helpers

    private TrassirIngestLogView toView(TrassirIngestLog l) {
        return new TrassirIngestLogView(
                l.getId(), l.getReceivedAt(), l.getDurationMs(),
                l.getHttpMethod(), l.getRequestUri(),
                // ✅ FIX: конвертируем обратно в String для фронта
                l.getRemoteAddr() != null ? l.getRemoteAddr().getHostAddress() : null,
                l.getXForwardedFor(),
                l.getContentType(), l.getContentLength(), l.getUserAgent(),
                l.isAuthTokenPresent(), l.getCorrelationId(),
                l.getRequestBodyPrefix(), l.getRequestBodyBytes(), l.getRequestBodySha256(),
                l.isRequestBodyTruncated(), l.isRequestBodyRead(),
                l.getHttpStatus(), l.getResponseCode(), l.getResponseMessage(), l.getResponseBody(),
                l.getBatchId(), l.getRawId(), l.getServerId(), l.getEventCount(),
                l.getCreatedAt());
    }

    private static InetAddress parseInet(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private HttpStatusInfo deriveResponse(IngestResult result, Throwable error) {
        if (error == null) {
            return new HttpStatusInfo(200, "OK",
                    result != null && result.duplicate() ? "Batch already processed" : "Accepted",
                    null);
        }
        if (error instanceof org.springframework.web.server.ResponseStatusException rse) {
            int status = rse.getStatusCode().value();
            String code = rse.getReason() != null ? rse.getReason() : "UNKNOWN";
            String message = humanMessage(code);
            String json = String.format(
                    "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\"}",
                    status, escapeJson(code), escapeJson(message));
            return new HttpStatusInfo(status, code, message, json);
        }
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = error.getMessage() != null ? error.getMessage() : "Unexpected error";
        String json = String.format(
                "{\"status\":500,\"code\":\"INTERNAL_ERROR\",\"message\":\"%s\"}",
                escapeJson(message));
        return new HttpStatusInfo(status, code, message, json);
    }

    private static String humanMessage(String code) {
        return switch (code) {
            case "INVALID_TRASSIR_ENVELOPE"  -> "Malformed JSON or wrong envelope structure";
            case "EMPTY_TRASSIR_BODY"        -> "Request body is empty";
            case "TRASSIR_BODY_TOO_LARGE"    -> "Request body exceeds size limit";
            case "UNSUPPORTED_TRASSIR_CONTRACT"         -> "Contract is not 'trassir.telemetry'";
            case "UNSUPPORTED_TRASSIR_CONTRACT_VERSION" -> "Unsupported contract_version";
            case "MISSING_TRASSIR_BATCH_ID"             -> "Missing batch_id";
            case "MISSING_TRASSIR_SERVER_ID"            -> "Missing server.server_id";
            case "MISSING_TRASSIR_PRODUCER"             -> "Missing producer.name";
            case "MISSING_TRASSIR_EVENTS"               -> "Missing events array";
            default -> code.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private static String nullIfBlank(String s) { return s == null || s.isBlank() ? null : s; }

    private static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String k = r[0] != null ? r[0].toString() : "null";
            m.put(k, ((Number) r[1]).longValue());
        }
        return m;
    }

    private record HttpStatusInfo(int status, String code, String message, String jsonBody) {}
}