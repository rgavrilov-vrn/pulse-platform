package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
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

        if (users.findByLogin(username).isPresent()) {
            log.info("Bootstrap: user '{}' already exists — skipping", username);
            return;
        }

        // FIX #88: fail-fast. Раньше — только warn при отсутствии/пустоте роли,
        // что давало «успешный логин без прав» и часы диагностики.
        // Apply V04__auth_seed.sql перед bootstrap.
        RoleEntity adminRole = roles.findByCodeIgnoreCase("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "Bootstrap: role ADMIN is missing. Apply V04__auth_seed.sql first."));
        if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
            throw new IllegalStateException(
                    "Bootstrap: role ADMIN has no permissions. Apply V04__auth_seed.sql first.");
        }

        UserEntity user = UserEntity.builder()
                .login(username)
                .passwordHash(encoder.encode(password))
                .displayName(username)
                .active(true)
                .build();

        // FIX #86: явный flush ДО assignRoles — id гарантированно заполнен
        // (prePersist → UUID), UserRoleId конструируется с non-null userId.
        // Раньше id=null спасал только @MapsId на UserRoleEntity — неявная
        // зависимость, ломающаяся при рефакторинге.
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // FIX #87: race — параллельный инстанс в K8s создал раньше.
            // Идемпотентно: продолжаем как «уже создан», без ошибки старта.
            if (isUniqueViolation(e, "users_login_key")) {
                log.warn("Bootstrap: user '{}' created by concurrent instance — skipping",
                        username);
                return;
            }
            throw e;
        }

        UserRoleEntity link = UserRoleEntity.builder()
                .id(new UserRoleId(user.getId(), adminRole.getId()))
                .user(user)
                .role(adminRole)
                .build();
        user.getRoles().add(link);
        users.saveAndFlush(user);

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

    /** FIX #87: распознаём race по SQLState 23505 + имя constraint. */
    private static boolean isUniqueViolation(Throwable t, String constraint) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                String msg = sql.getMessage();
                return msg != null && msg.contains(constraint);
            }
        }
        return false;
    }
}