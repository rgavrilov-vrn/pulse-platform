package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.auth.domain.RoleEntity;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}