package ru.agroeco.pulse.trassir.application;

public interface TrassirEventUseCase {

    EventProcessingResult handle(TrassirEventContext context);
}
