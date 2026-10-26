package ru.agroeco.pulse.platform.adapter.persistence.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);
}