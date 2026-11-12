package ru.agroeco.pulse.auth.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agroeco.pulse.auth.domain.RevokedJtiEntity;

import java.time.Instant;

public interface RevokedJtiRepository extends JpaRepository<RevokedJtiEntity, String> {

    boolean existsByJti(String jti);

    @Modifying
    @Query("delete from RevokedJtiEntity r where r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}