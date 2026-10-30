package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.auth.domain.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Для аутентификации и JWT-фильтра.
     *
     * login — CITEXT: сравнение регистронезависимое на уровне типа,
     * запрос использует btree-индекс auth.users.login. Вариант
     * findByLoginIgnoreCase генерировал LOWER(login) и full-scan'ил.
     *
     * EntityGraph тянет всю цепочку user → roles → role → role_permissions
     * → permission за один проход, чтобы AppPrincipalMapper не делал N+1
     * на каждый аутентифицированный HTTP-запрос.
     */
    @EntityGraph(attributePaths = {
            "roles",
            "roles.role",
            "roles.role.permissions",
            "roles.role.permissions.permission"
    })
    Optional<UserEntity> findByLogin(String login);

    boolean existsByLogin(String login);
}