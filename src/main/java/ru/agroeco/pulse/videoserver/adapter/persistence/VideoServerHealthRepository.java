package ru.agroeco.pulse.videoserver.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.videoserver.domain.VideoServerHealth;

import java.util.UUID;

public interface VideoServerHealthRepository extends JpaRepository<VideoServerHealth, UUID> {
}