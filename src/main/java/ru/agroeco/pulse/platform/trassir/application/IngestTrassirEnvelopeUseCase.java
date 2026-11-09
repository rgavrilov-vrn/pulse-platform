package ru.agroeco.pulse.platform.trassir.application;

public interface IngestTrassirEnvelopeUseCase {

    void handle(String rawBody);
}
