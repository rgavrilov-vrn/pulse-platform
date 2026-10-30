package ru.agroeco.pulse.incident.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.incident.domain.CommentEntity;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    Page<CommentEntity> findByIncidentIdAndDeletedFalse(UUID incidentId, Pageable pageable);
}