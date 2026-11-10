package ru.agroeco.pulse.trassir.application;

public interface IngestTrassirEnvelopeUseCase {

    void handle(String rawBody);
}
