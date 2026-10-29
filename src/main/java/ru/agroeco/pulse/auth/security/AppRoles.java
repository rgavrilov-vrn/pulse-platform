package ru.agroeco.pulse.auth.security;

/**
 * Коды ролей — константы для @PreAuthorize("hasRole(...)") и bootstrap.
 * Сами права выдаются через auth.role_permissions (data-driven), НЕ в этом классе.
 */
public final class AppRoles {

    public static final String SUPERUSER   = "SUPERUSER";
    public static final String ADMIN       = "ADMIN";
    public static final String OPERATOR    = "OPERATOR";
    public static final String REVIEWER    = "REVIEWER";
    public static final String VIEWER      = "VIEWER";
    public static final String INTEGRATION = "INTEGRATION";

    private AppRoles() {}
}