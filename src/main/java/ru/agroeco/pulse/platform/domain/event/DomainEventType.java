package ru.agroeco.pulse.platform.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum DomainEventType {

    // ---- auth ----
    USER_AUTHENTICATED("USER_AUTHENTICATED"),
    USER_AUTHENTICATION_FAILED("USER_AUTHENTICATION_FAILED"),
    USER_LOGGED_OUT("USER_LOGGED_OUT"),

    // ---- user management ----
    USER_CREATED("USER_CREATED"),
    USER_ROLE_CHANGED("USER_ROLE_CHANGED"),
    USER_DISABLED("USER_DISABLED"),

    // ---- incident ----
    INCIDENT_CREATED("INCIDENT_CREATED"),
    INCIDENT_STATUS_CHANGED("INCIDENT_STATUS_CHANGED"),
    INCIDENT_COMMENT_ADDED("INCIDENT_COMMENT_ADDED"),

    // ---- videoserver ----
    VIDEO_EVENT_INGESTED("VIDEO_EVENT_INGESTED"),

    // ---- refs: facilities (ref.objects) ----
    FACILITY_CREATED("FACILITY_CREATED"),

    // ---- refs: zones ----
    ZONE_CREATED("ZONE_CREATED"),

    // ---- refs: departments ----
    DEPARTMENT_CREATED("DEPARTMENT_CREATED"),

    // ---- refs: employees ----
    EMPLOYEE_CREATED("EMPLOYEE_CREATED")
    ;

    private final String value;

    public static Optional<DomainEventType> fromValue(String value) {
        if (value == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(a -> a.value.equals(value))
                .findFirst();
    }
}