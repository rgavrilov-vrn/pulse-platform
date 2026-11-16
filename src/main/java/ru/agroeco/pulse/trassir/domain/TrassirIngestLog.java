package ru.agroeco.pulse.trassir.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingest_log", schema = "trassir")
@Getter @Setter @NoArgsConstructor
public class TrassirIngestLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @Column(name = "duration_ms", nullable = false) private Integer durationMs;

    // ---- HTTP request
    @Column(name = "http_method", nullable = false, length = 10) private String httpMethod;
    @Column(name = "request_uri", nullable = false, columnDefinition = "text") private String requestUri;

    // ✅ FIX: InetAddress + INET
    @Column(name = "remote_addr", columnDefinition = "inet")
    @JdbcTypeCode(SqlTypes.INET)
    private InetAddress remoteAddr;

    @Column(name = "x_forwarded_for", columnDefinition = "text") private String xForwardedFor;
    @Column(name = "content_type", length = 128)                 private String contentType;
    @Column(name = "content_length")                             private Long contentLength;
    @Column(name = "user_agent", columnDefinition = "text")      private String userAgent;
    @Column(name = "auth_token_present", nullable = false)       private boolean authTokenPresent;
    @Column(name = "correlation_id", length = 100)               private String correlationId;

    // ---- request body
    @Column(name = "request_body_prefix", columnDefinition = "text") private String requestBodyPrefix;
    @Column(name = "request_body_bytes")                             private Long requestBodyBytes;

    @Column(name = "request_body_sha256", columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestBodySha256;

    @Column(name = "request_body_truncated", nullable = false)       private boolean requestBodyTruncated;
    @Column(name = "request_body_read", nullable = false)            private boolean requestBodyRead;

    // ---- response
    @Column(name = "http_status", nullable = false)                private Integer httpStatus;
    @Column(name = "response_code", length = 64)                   private String responseCode;
    @Column(name = "response_message", columnDefinition = "text")  private String responseMessage;
    @Column(name = "response_body", columnDefinition = "text")     private String responseBody;

    // ---- relations
    @Column(name = "batch_id")    private UUID batchId;
    @Column(name = "raw_id")      private UUID rawId;
    @Column(name = "server_id")   private UUID serverId;
    @Column(name = "event_count") private Integer eventCount;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}