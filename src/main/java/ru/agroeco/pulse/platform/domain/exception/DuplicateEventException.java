package ru.agroeco.pulse.platform.domain.exception;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String m) {
        super(m);
    }
}
