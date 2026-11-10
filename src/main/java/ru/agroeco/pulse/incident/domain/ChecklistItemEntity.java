package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "checklist_items", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChecklistItemEntity {

    @Id private UUID id;
    @Column(name = "category_id", nullable = false)          private UUID categoryId;
    @Column(nullable = false, length = 100, unique = true)   private String code;
    @Column(nullable = false, length = 500)                  private String name;
    @Column(columnDefinition = "text")                       private String description;
    @Column(name = "requires_image",   nullable = false)     private boolean requiresImage;
    @Column(name = "requires_comment", nullable = false)     private boolean requiresComment;
    @Column(name = "requires_video",   nullable = false)     private boolean requiresVideo;

    // FIX #66: enum вместо String.
    @Enumerated(EnumType.STRING)
    @Column(name = "default_severity", nullable = false, length = 32)
    private IncidentSeverity defaultSeverity;

    @Column(name = "comment_template", columnDefinition = "text") private String commentTemplate;
    @Column(name = "sort_order", nullable = false)           private int sortOrder;
    @Column(name = "is_active", nullable = false)            private boolean active;

    // FIX #70: timestamps — раньше отсутствовали, при UPDATE updated_at
    // в БД не обновлялся триггером (его нет в V10), а в entity его не было вовсе.
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }
}