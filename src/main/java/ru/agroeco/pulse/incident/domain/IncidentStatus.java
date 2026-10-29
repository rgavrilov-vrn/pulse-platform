// ru.agroeco.pulse.incident.domain.IncidentStatus
package ru.agroeco.pulse.incident.domain;

public enum IncidentStatus {
    DRAFT, RECORDED, NEEDS_REVIEW, CONFIRMED, REJECTED, CLOSED, CANCELLED, ARCHIVED;
    public boolean isTerminal() {
        return this == ARCHIVED || this == CANCELLED || this == REJECTED;
    }
}