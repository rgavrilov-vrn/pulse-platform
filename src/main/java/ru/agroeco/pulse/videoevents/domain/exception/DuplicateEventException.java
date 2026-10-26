package ru.agroeco.pulse.videoevents.domain.exception;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String m) {
        super(m);
    }
}
