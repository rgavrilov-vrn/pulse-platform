package ru.agroeco.pulse.videoserver.api;

public enum VideoEventType {
    MOTION_DETECTED,
    CAMERA_OFFLINE,
    CAMERA_TAMPERING,
    VIDEO_EVENT_RECEIVED,
    UNKNOWN
}