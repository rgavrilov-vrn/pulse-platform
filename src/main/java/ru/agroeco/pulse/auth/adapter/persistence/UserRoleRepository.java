package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.auth.domain.UserRoleEntity;
import ru.agroeco.pulse.auth.domain.UserRoleId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    List<UserRoleEntity> findByUserId(UUID userId);

    List<UserRoleEntity> findByRoleId(UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    /** Батч-загрузка ролей для списка пользователей. */
    @EntityGraph(attributePaths = {"role"})
    List<UserRoleEntity> findByUserIdIn(Collection<UUID> userIds);

    /** Защита от lockout: сколько ещё пользователей имеют указанную роль. */
    @Query("SELECT count(DISTINCT ur.user.id) FROM UserRoleEntity ur WHERE ur.role.code = :code")
    long countDistinctUsersByRoleCode(@Param("code") String code);
}