package ru.agroeco.pulse.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import ru.agroeco.pulse.platform.adapter.persistence.security.UserEntity;

import java.util.ArrayList;
import java.util.List;

@Component
public class AppPrincipalMapper {

    public AppPrincipal toPrincipal(UserEntity user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

//        userRole.getRole().getPermissions().forEach(permission ->
//                authorities.add(new SimpleGrantedAuthority(permission.getCode()))
//        );

        user.getRoles().forEach(userRole -> {
            String roleCode = userRole.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode)) {
                authorities.add(new SimpleGrantedAuthority("user:manage"));
            }
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()));
        });

        return new AppPrincipal(
                user.getId(),
                user.getLogin(),
                user.getPasswordHash(),
                authorities
        );
    }
}