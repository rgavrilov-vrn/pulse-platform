package ru.agroeco.pulse.auth.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.auth.domain.RoleEntity;
import ru.agroeco.pulse.auth.domain.RolePermissionEntity;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;
import ru.agroeco.pulse.platform.security.AppPrincipal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class AppPrincipalMapper {

    public AppPrincipal toPrincipal(UserEntity user) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (UserRoleEntity userRole : user.getRoles()) {
            RoleEntity role = userRole.getRole();
            String roleCode = role.getCode();

            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + roleCode.toUpperCase(Locale.ROOT)));

            for (RolePermissionEntity rp : role.getPermissions()) {
                if (rp.getPermission() == null) {
                    log.warn("Role '{}' has dangling role_permission record without permission — skipped",
                            roleCode);
                    continue;
                }
                authorities.add(new SimpleGrantedAuthority(rp.getPermission().getCode()));
            }
        }

        return new AppPrincipal(
                user.getId(),
                user.getLogin(),
                user.getPasswordHash(),
                new ArrayList<>(authorities));
    }
}