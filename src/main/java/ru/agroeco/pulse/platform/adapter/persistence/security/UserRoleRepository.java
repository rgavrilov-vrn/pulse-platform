package ru.agroeco.pulse.platform.adapter.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    List<UserRoleEntity> findByUserId(UUID userId);

    List<UserRoleEntity> findByRoleId(UUID roleId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
}