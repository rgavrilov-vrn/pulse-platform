package ru.agroeco.pulse.videoevents.domain.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String m) {
        super(m);
    }
}
