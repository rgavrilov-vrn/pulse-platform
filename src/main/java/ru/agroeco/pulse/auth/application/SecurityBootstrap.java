package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.agroeco.pulse.auth.adapter.persistence.RoleRepository;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.domain.RoleEntity;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;
import ru.agroeco.pulse.auth.domain.UserRoleId;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.security.bootstrap.enabled", havingValue = "true")
public class SecurityBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Value("${app.security.bootstrap.username:}") private String username;
    @Value("${app.security.bootstrap.password:}") private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateInput();

        if (users.findByLoginIgnoreCase(username).isPresent()) {
            log.info("Bootstrap: user '{}' already exists — skipping", username);
            return;
        }

        RoleEntity adminRole = roles.findByCodeIgnoreCase("ADMIN")
                .orElseGet(() -> {
                    log.warn("Bootstrap: role ADMIN is missing — creating empty role. "
                            + "Run RBAC seed migration (003_1) to populate permissions.");
                    return roles.save(RoleEntity.builder()
                            .code("ADMIN")
                            .name("Administrator")
                            .description("Full administrative access")
                            .build());
                });

        if (adminRole.getPermissions().isEmpty()) {
            log.warn("Bootstrap: role ADMIN has no permissions. "
                    + "Apply migration 003_1_create_auth_permissions.sql before production use.");
        }

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

        log.info("Bootstrap: admin user '{}' created with role ADMIN", username);
    }

    private void validateInput() {
        if (username == null || username.isBlank()
                || password == null || password.length() < 16
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Bootstrap requires username and a 16+ character password of at most 72 UTF-8 bytes");
        }
    }
}