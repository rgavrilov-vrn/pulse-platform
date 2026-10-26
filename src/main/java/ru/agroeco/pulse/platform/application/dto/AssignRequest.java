package ru.agroeco.pulse.platform.application.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AssignRequest(@NotNull UUID assigneeId, Instant dueAt) {
}
