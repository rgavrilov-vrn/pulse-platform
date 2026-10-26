package ru.agroeco.pulse.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.agroeco.pulse.platform.adapter.persistence.security.RoleEntity;
import ru.agroeco.pulse.platform.adapter.persistence.security.RoleRepository;
import ru.agroeco.pulse.platform.adapter.persistence.security.UserEntity;
import ru.agroeco.pulse.platform.adapter.persistence.security.UserRepository;
import ru.agroeco.pulse.platform.adapter.persistence.security.UserRoleEntity;
import ru.agroeco.pulse.platform.adapter.persistence.security.UserRoleId;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.bootstrap.enabled", havingValue = "true")
public class SecurityBootstrap implements ApplicationRunner {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Value("${app.security.bootstrap.username:}")
    private String username;

    @Value("${app.security.bootstrap.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateInput();

        if (users.findByLoginIgnoreCase(username).isPresent()) {
            return;
        }

        RoleEntity adminRole = roles.findByCodeIgnoreCase(ADMIN_ROLE_CODE)
                .orElseGet(() -> createAdminRole());

        UserEntity user = UserEntity.builder()
                .login(username)
                .passwordHash(encoder.encode(password))
                .displayName(username)
                .active(true)
                .build();

        UserRoleEntity userRole = UserRoleEntity.builder()
                .id(new UserRoleId(user.getId(), adminRole.getId()))
                .user(user)
                .role(adminRole)
                .build();

        user.getRoles().add(userRole);
        users.save(user);
    }

    private void validateInput() {
        if (username == null || username.isBlank()
                || password == null || password.length() < 16
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Bootstrap requires username and a 16+ character password of at most 72 UTF-8 bytes"
            );
        }
    }

    private RoleEntity createAdminRole() {
        return roles.save(RoleEntity.builder()
                .code(ADMIN_ROLE_CODE)
                .name("Administrator")
                .description("Full administrative access")
                .build());
    }
}