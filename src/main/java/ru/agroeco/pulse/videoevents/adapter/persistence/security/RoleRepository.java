package ru.agroeco.pulse.videoevents.adapter.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}