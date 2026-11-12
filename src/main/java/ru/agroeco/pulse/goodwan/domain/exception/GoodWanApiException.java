package ru.agroeco.pulse.goodwan.domain.exception;

public class GoodWanApiException extends RuntimeException {

    public GoodWanApiException(String message) {
        super(message);
    }

    public GoodWanApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
