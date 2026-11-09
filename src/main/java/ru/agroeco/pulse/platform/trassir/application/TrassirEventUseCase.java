package ru.agroeco.pulse.platform.trassir.application;

public interface TrassirEventUseCase {

    EventProcessingResult handle(TrassirEventContext context);
}
