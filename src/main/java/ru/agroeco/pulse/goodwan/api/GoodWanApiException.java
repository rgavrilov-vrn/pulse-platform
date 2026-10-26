package ru.agroeco.pulse.goodwan.api;

public class GoodWanApiException extends RuntimeException {

    public GoodWanApiException(String message) {
        super(message);
    }

    public GoodWanApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
