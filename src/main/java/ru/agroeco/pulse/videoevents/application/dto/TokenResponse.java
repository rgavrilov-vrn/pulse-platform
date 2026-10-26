package ru.agroeco.pulse.videoevents.application.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
