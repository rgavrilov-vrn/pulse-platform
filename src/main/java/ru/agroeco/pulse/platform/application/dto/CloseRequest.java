package ru.agroeco.pulse.platform.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseRequest(@NotBlank String resolutionComment) {
}
