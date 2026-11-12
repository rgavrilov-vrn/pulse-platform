package ru.agroeco.pulse.goodwan.adapter.http.dto;

public record GoodWanAuthenticationStatus(
    boolean authenticated,
    int httpStatus,
    String message
) {
}
