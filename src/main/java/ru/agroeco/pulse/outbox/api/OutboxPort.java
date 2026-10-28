package ru.agroeco.pulse.outbox.api;

public interface OutboxPort {

    void publish(OutboxMessage message);
}
