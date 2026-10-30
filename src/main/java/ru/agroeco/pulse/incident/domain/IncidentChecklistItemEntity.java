package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_checklist_items", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentChecklistItemEntity {

    @Id
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "checklist_item_id", nullable = false)
    private UUID checklistItemId;

    @Column(name = "item_name_snapshot", nullable = false, length = 500)
    private String itemNameSnapshot;

    @Column(name = "category_name_snapshot", length = 255)
    private String categoryNameSnapshot;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }
}