package ru.agroeco.pulse.platform.security;

import java.util.Set;

public enum AppRole {
    OPERATOR(Set.of("incident:read", "incident:evidence")),
    REVIEWER(Set.of("incident:read", "incident:confirm", "incident:reject", "incident:assign")),
    RESPONSIBLE(Set.of("incident:read:assigned", "incident:evidence:assigned", "incident:close:assigned")),
    AUDITOR(Set.of("incident:read", "audit:read")),
    INTEGRATION(Set.of("video-event:ingest")),
    ADMIN(Set.of("incident:read", "incident:evidence", "incident:confirm", "incident:reject",
            "incident:assign", "incident:close", "audit:read", "video-event:ingest", "user:manage"));

    private final Set<String> permissions;

    AppRole(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Set<String> permissions() {
        return permissions;
    }
}
