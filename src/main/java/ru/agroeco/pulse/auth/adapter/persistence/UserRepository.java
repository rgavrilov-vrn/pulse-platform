package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.auth.domain.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = {
            "roles",
            "roles.role",
            "roles.role.permissions",
            "roles.role.permissions.permission"
    })
    Optional<UserEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    /**
     * Поиск для админского списка. Без {@code @EntityGraph} — роли
     * догружаются отдельным батч-запросом в сервисе,
     * чтобы избежать cartesian product с {@code Pageable}.
     */
    @Query("""
            SELECT u FROM UserEntity u
            WHERE (:enabled IS NULL OR u.active = :enabled)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.login)       LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<UserEntity> search(@Param("q") String q,
                            @Param("enabled") Boolean enabled,
                            Pageable pageable);
}