// ru.agroeco.pulse.incident.domain.IncidentKind
package ru.agroeco.pulse.incident.domain;

public enum IncidentKind {
    EVENT, VIOLATION;

    public static IncidentKind fromGroupCode(String code) {
        return switch (code) {
            case "EVENT"     -> EVENT;
            case "VIOLATION" -> VIOLATION;
            default -> throw new IllegalArgumentException("Unknown group code: " + code);
        };
    }

    public String toGroupCode() { return name(); }
}