package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.auth.domain.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /** Для аутентификации: сразу тянем роли (без cartesian product). */
    @EntityGraph(attributePaths = {"roles", "roles.role"})
    Optional<UserEntity> findByLoginIgnoreCase(String login);

    /** Для админских операций — без eager-загрузки, чтобы не дублировать. */
    boolean existsByLoginIgnoreCase(String login);
}