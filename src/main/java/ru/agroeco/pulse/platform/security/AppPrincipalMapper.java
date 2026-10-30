package ru.agroeco.pulse.platform.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.auth.domain.RoleEntity;
import ru.agroeco.pulse.auth.domain.RolePermissionEntity;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Строит AppPrincipal из UserEntity.
 * Права берутся ТОЛЬКО из БД: user_roles → roles → role_permissions → permissions.
 * Никаких хардкод-маппингов, как было раньше для ADMIN.
 */
@Component
@Slf4j
public class AppPrincipalMapper {

    public AppPrincipal toPrincipal(UserEntity user) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (UserRoleEntity userRole : user.getRoles()) {
            RoleEntity role = userRole.getRole();
            String roleCode = role.getCode();

            // 1) ROLE_<CODE> — для @PreAuthorize("hasRole('ADMIN')")
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));

            // 2) Гранулярные permissions — для @PreAuthorize("hasAuthority('incident:read')")
            for (RolePermissionEntity rp : role.getPermissions()) {
                if (rp.getPermission() == null) {
                    log.warn("Role '{}' has dangling role_permission record without permission — skipped", roleCode);
                    continue;
                }
                authorities.add(new SimpleGrantedAuthority(rp.getPermission().getCode()));
            }
        }

        return new AppPrincipal(
                user.getId(),
                user.getLogin(),
                user.getPasswordHash(),
                new ArrayList<>(authorities)
        );
    }
}