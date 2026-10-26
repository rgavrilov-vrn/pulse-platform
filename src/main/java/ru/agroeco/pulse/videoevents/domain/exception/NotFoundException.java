package ru.agroeco.pulse.videoevents.domain.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String m) {
        super(m);
    }
}
