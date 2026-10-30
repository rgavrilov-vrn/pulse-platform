package ru.agroeco.pulse.refs.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateEmployeeRequest(
        @Size(max = 100) String personnelNo,      // nullable, но unique
        @NotBlank @Size(max = 255) String fullName,
        UUID departmentId,                        // nullable
        Boolean isActive
) {
}