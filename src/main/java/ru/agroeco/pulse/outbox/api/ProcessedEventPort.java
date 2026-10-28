package ru.agroeco.pulse.outbox.api;

import java.util.UUID;

public interface ProcessedEventPort {

    boolean tryMarkProcessed(String consumerName, UUID eventId);
}
