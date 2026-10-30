package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Table(name = "checklist_items", schema = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChecklistItemEntity {

    @Id private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "requires_image",   nullable = false) private boolean requiresImage;
    @Column(name = "requires_comment", nullable = false) private boolean requiresComment;
    @Column(name = "requires_video",   nullable = false) private boolean requiresVideo;

    @Column(name = "default_severity", nullable = false, length = 32)
    private String defaultSeverity;

    @Column(name = "comment_template", columnDefinition = "text")
    private String commentTemplate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }
}