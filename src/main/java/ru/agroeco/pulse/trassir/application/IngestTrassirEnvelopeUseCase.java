package ru.agroeco.pulse.trassir.application;

public interface IngestTrassirEnvelopeUseCase {

    IngestResult handle(String rawBody);
}