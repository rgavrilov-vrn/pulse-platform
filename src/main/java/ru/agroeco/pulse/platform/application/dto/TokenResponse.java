package ru.agroeco.pulse.platform.application.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
