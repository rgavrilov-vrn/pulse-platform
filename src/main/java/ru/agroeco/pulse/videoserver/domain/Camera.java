package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "cameras", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Camera {

    @Id private UUID id;

    @Column(name = "video_server_id", nullable = false) private UUID videoServerId;
    @Column(nullable = false, length = 64)              private String source;
    @Column(name = "external_id", length = 255)         private String externalId;
    @Column(length = 100)                                private String code;
    @Column(nullable = false, length = 255)             private String name;
    private String description;

    @Enumerated(EnumType.STRING) @Column(length = 32)   private CameraStatus status;
    @Column(name = "is_active", nullable = false)       private boolean isActive = true;

    @Column(name = "last_seen_at")     private Instant lastSeenAt;
    @Column(name = "deleted_at")       private Instant deletedAt;
    @Column(name = "deactivated_by", length = 32) private String deactivatedBy;

    // state-tracking
    @Column(name = "last_connection_status", length = 64) private String lastConnectionStatus;
    @Column(name = "last_connection_at")                  private Instant lastConnectionAt;
    @Column(name = "last_ip_address")                     private String lastIpAddress;
    @Column(name = "last_model")                          private String lastModel;
    @Column(name = "last_family")                         private String lastFamily;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false)                    private Instant updatedAt;

    public Camera(UUID id, UUID videoServerId, String source, String externalId,
                  String code, String name, String description,
                  CameraStatus status, boolean isActive) {
        this.id = id;
        this.videoServerId = videoServerId;
        this.source = source;
        this.externalId = externalId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = status;
        this.isActive = isActive;
    }

    public CameraSnapshotDiff applySnapshotWithDiff(String name, String code, String description,
                                                    Instant seenAt, String connectionStatus,
                                                    String ipAddress, String model, String family) {
        String previousConnection = this.lastConnectionStatus;
        CameraSnapshotDiff diff = new CameraSnapshotDiff(previousConnection, connectionStatus);

        this.name = name;
        this.code = code;
        this.description = description;
        this.lastSeenAt = seenAt;
        this.lastConnectionAt = seenAt;
        this.lastConnectionStatus = connectionStatus;
        this.lastIpAddress = ipAddress;
        this.lastModel = model;
        this.lastFamily = family;
        this.deletedAt = null;
        this.deactivatedBy = null;
        this.isActive = true;

        // ✅ маппинг values.connection → CameraStatus
        this.status = mapConnectionToStatus(connectionStatus);

        return diff;
    }

    public void applySnapshot(String name, String code, String description, Instant seenAt) {
        applySnapshotWithDiff(name, code, description, seenAt,
                this.lastConnectionStatus, this.lastIpAddress, this.lastModel, this.lastFamily);
    }

    public void updateManually(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public void softDeleteByAgent(Instant at) {
        this.deletedAt = at;
        this.deactivatedBy = "agent_snapshot";
        this.isActive = false;
    }

    public void softDeleteManually(Instant at) {
        this.deletedAt = at;
        this.deactivatedBy = "manual";
        this.isActive = false;
    }

    /**
     * values.connection → CameraStatus.
     * "Disconnected" проверяем раньше "Connected", т.к. содержит подстроку.
     */
    private static CameraStatus mapConnectionToStatus(String connection) {
        if (connection == null || connection.isBlank()) return CameraStatus.UNKNOWN;
        String lower = connection.toLowerCase(Locale.ROOT).trim();
        if (lower.contains("disconnect"))                             return CameraStatus.OFFLINE;
        if (lower.contains("needs attention") || lower.contains("error")) return CameraStatus.OFFLINE;
        if (lower.contains("connect"))                                return CameraStatus.ONLINE;
        return CameraStatus.UNKNOWN;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = CameraStatus.UNKNOWN;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public record CameraSnapshotDiff(String previousConnection, String currentConnection) {
        public boolean isDegradedTransition() {
            return "Connected".equalsIgnoreCase(previousConnection)
                    && !"Connected".equalsIgnoreCase(currentConnection);
        }
        public boolean isRecoveryTransition() {
            return previousConnection != null
                    && !"Connected".equalsIgnoreCase(previousConnection)
                    && "Connected".equalsIgnoreCase(currentConnection);
        }
        public boolean isOffline() {
            return "Disconnected".equalsIgnoreCase(currentConnection);
        }
    }
}