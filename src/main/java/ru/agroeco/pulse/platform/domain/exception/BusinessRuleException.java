package ru.agroeco.pulse.platform.domain.exception;

public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String m) {
        super(m);
    }
}
