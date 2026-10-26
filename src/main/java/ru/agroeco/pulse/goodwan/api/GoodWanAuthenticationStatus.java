package ru.agroeco.pulse.goodwan.api;

public record GoodWanAuthenticationStatus(
    boolean authenticated,
    int httpStatus,
    String message
) {
}
