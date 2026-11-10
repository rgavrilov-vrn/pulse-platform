package ru.agroeco.pulse.platform.trassir.application;

/** Temporary non-bean delegate; remove after migration. */
@Deprecated
public class IngestTrassirEnvelopeUseCaseImpl implements IngestTrassirEnvelopeUseCase {

    private final ru.agroeco.pulse.trassir.application.IngestTrassirEnvelopeUseCase delegate;

    public IngestTrassirEnvelopeUseCaseImpl(ru.agroeco.pulse.trassir.application.IngestTrassirEnvelopeUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handle(String rawBody) {
        delegate.handle(rawBody);
    }
}
