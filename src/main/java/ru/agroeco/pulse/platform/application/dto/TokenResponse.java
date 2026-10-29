package ru.agroeco.pulse.platform.application.dto;

import lombok.Builder;
import lombok.With;

@Builder
@With
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
