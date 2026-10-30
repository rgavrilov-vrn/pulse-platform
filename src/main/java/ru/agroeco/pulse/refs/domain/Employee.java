package ru.agroeco.pulse.refs.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "employees",
        schema = "ref",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_employees_personnel_no", columnNames = "personnel_no")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    private UUID id;

    @Column(name = "personnel_no", length = 100)
    private String personnelNo;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Employee(UUID id, String personnelNo, String fullName,
                    UUID departmentId, boolean isActive) {
        this.id = id;
        this.personnelNo = personnelNo;
        this.fullName = fullName;
        this.departmentId = departmentId;
        this.isActive = isActive;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}