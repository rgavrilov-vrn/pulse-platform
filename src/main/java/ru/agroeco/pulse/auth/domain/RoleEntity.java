// Изменения внутри класса (остальные поля — без изменений):
package ru.agroeco.pulse.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", schema = "auth")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // НОВОЕ: связка role ↔ permissions
    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RolePermissionEntity> permissions = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}