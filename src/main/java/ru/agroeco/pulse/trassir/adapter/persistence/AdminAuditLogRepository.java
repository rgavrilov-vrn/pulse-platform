package ru.agroeco.pulse.trassir.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.agroeco.pulse.trassir.domain.AdminAuditLog;

import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository
        extends JpaRepository<AdminAuditLog, UUID>,
        JpaSpecificationExecutor<AdminAuditLog> {

    @Query("SELECT DISTINCT a.action FROM AdminAuditLog a ORDER BY a.action")
    List<String> distinctActions();

    @Query("SELECT DISTINCT a.actor FROM AdminAuditLog a ORDER BY a.actor")
    List<String> distinctActors();
}