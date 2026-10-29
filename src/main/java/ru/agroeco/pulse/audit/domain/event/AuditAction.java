package ru.agroeco.pulse.audit.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum AuditAction {

    // ---- auth ----
    USER_AUTHENTICATED("USER_AUTHENTICATED"),
    USER_AUTHENTICATION_FAILED("USER_AUTHENTICATION_FAILED"),
    USER_LOGGED_OUT("USER_LOGGED_OUT"),

    // ---- User action ------
    USER_CREATED("USER_CREATED"),
    USER_ROLE_CHANGED("USER_ROLE_CHANGED"),
    USER_DISABLED("USER_DISABLED"),

    INCIDENT_CREATED ("INCIDENT_CREATED"),
    INCIDENT_STATUS_CHANGED ("INCIDENT_STATUS_CHANGED"),
    INCIDENT_COMMENT_ADDED ("INCIDENT_COMMENT_ADDED"),

    // ---- videoserver ----
    VIDEO_EVENT_INGESTED("VIDEO_EVENT_INGESTED")
    ;

    private final String value;

    public static Optional<AuditAction> fromValue(String value) {
        if (value == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(a -> a.value.equals(value))
                .findFirst();
    }
}
