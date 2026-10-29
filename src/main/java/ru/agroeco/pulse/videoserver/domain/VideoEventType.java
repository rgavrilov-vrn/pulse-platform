package ru.agroeco.pulse.videoserver.domain;

public enum VideoEventType {
    MOTION_DETECTED,
    CAMERA_OFFLINE,
    CAMERA_TAMPERING,
    VIDEO_EVENT_RECEIVED,
    UNKNOWN
}