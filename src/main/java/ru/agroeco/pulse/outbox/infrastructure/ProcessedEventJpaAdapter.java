package ru.agroeco.pulse.outbox.infrastructure;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.outbox.infrastructure.persistence.ProcessedEventRepository;

@Component
public class ProcessedEventJpaAdapter implements ProcessedEventPort {

    private final ProcessedEventRepository repository;

    public ProcessedEventJpaAdapter(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean tryMarkProcessed(String consumerName, UUID eventId) {
        return repository.insertIfAbsent(consumerName, eventId, Instant.now()) == 1;
    }
}
