package ru.agroeco.pulse.auth.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Настройки управления ролями.
 *
 * <p>Защищённые роли требуют authority {@code system:admin} для выдачи.
 * По умолчанию — {@code SUPERUSER} и {@code ADMIN}: обычный оператор
 * с {@code user:assign_role} не должен иметь возможность повысить
 * привилегии себе или другому пользователю.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.role-assignment")
public class RoleAssignmentProperties {

    private Set<String> protectedRoles = new LinkedHashSet<>(Set.of("SUPERUSER", "ADMIN"));

    /** Authority, требуемая для выдачи защищённых ролей. */
    private String protectedRoleAuthority = "system:admin";
}