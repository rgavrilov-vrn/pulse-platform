package ru.agroeco.pulse.videoserver.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channels", schema = "videoserver")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id private UUID id;
    @Column(name = "camera_id", nullable = false)       private UUID cameraId;
    @Column(name = "external_id", length = 255)         private String externalId;
    @Column(nullable = false, length = 255)             private String name;
    @Enumerated(EnumType.STRING) @Column(length = 32)   private ChannelStatus status;
    @Column(name = "is_active", nullable = false)       private boolean isActive = true;
    @Column(name = "last_seen_at")                      private Instant lastSeenAt;

    // state-tracking
    @Column(name = "last_signal")          private Boolean lastSignal;
    @Column(name = "last_recording_local") private Boolean lastRecordingLocal;
    @Column(name = "last_grabber_error", length = 255) private String lastGrabberError;
    @Column(name = "last_fps_main")        private Float lastFpsMain;
    @Column(name = "last_kbps_main")       private Float lastKbpsMain;
    @Column(name = "last_signal_at")       private Instant lastSignalAt;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false)                    private Instant updatedAt;

    public Channel(UUID id, UUID cameraId, String externalId, String name,
                   ChannelStatus status, boolean isActive) {
        this.id = id;
        this.cameraId = cameraId;
        this.externalId = externalId;
        this.name = name;
        this.status = status;
        this.isActive = isActive;
    }

    public ChannelSnapshotDiff applySnapshotWithDiff(String name, ChannelStatus status,
                                                     Instant seenAt, Boolean signal,
                                                     Boolean recordingLocal, String grabberError,
                                                     Float fpsMain, Float kbpsMain) {
        ChannelSnapshotDiff diff = new ChannelSnapshotDiff(
                this.lastSignal, signal,
                this.lastRecordingLocal, recordingLocal,
                this.lastGrabberError, grabberError);

        this.name = name;
        this.status = status;
        this.lastSeenAt = seenAt;
        this.lastSignalAt = seenAt;
        this.lastSignal = signal;
        this.lastRecordingLocal = recordingLocal;
        this.lastGrabberError = grabberError;
        this.lastFpsMain = fpsMain;
        this.lastKbpsMain = kbpsMain;
        this.isActive = true;

        return diff;
    }

    public void applySnapshot(String name, ChannelStatus status, Instant seenAt) {
        applySnapshotWithDiff(name, status, seenAt,
                this.lastSignal, this.lastRecordingLocal, this.lastGrabberError,
                this.lastFpsMain, this.lastKbpsMain);
    }

    public void updateManually(String name, ChannelStatus status) {
        this.name = name;
        this.status = status;
    }

    public void softDeleteManually(Instant at) {
        this.isActive = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public record ChannelSnapshotDiff(
            Boolean previousSignal, Boolean currentSignal,
            Boolean previousRecordingLocal, Boolean currentRecordingLocal,
            String previousGrabberError, String currentGrabberError) {

        public boolean isStreamLost() {
            return Boolean.TRUE.equals(previousSignal) && Boolean.FALSE.equals(currentSignal);
        }
        public boolean isStreamRestored() {
            return Boolean.FALSE.equals(previousSignal) && Boolean.TRUE.equals(currentSignal);
        }
        public boolean isRecordingStopped() {
            return Boolean.TRUE.equals(previousRecordingLocal) && Boolean.FALSE.equals(currentRecordingLocal);
        }
    }
}