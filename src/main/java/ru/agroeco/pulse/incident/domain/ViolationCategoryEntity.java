package ru.agroeco.pulse.incident.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "violation_categories", schema = "incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationCategoryEntity {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }
}