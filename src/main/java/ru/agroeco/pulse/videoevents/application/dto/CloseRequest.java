package ru.agroeco.pulse.videoevents.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseRequest(@NotBlank String resolutionComment) {
}
