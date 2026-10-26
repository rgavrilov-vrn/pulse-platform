package ru.agroeco.pulse.videoevents.domain;

public enum IncidentStatus {
    RECORDED, NEEDS_REVIEW, CONFIRMED, REJECTED, CLOSED, CANCELLED;

    public boolean canTransitionTo(IncidentStatus t) {
        return switch (this) {
            case RECORDED, NEEDS_REVIEW -> t == CONFIRMED || t == REJECTED || t == CANCELLED;
            case CONFIRMED, REJECTED -> t == CLOSED || t == CANCELLED;
            default -> false;
        };
    }
}
