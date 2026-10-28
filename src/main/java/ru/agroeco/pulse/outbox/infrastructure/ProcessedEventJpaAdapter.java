package ru.agroeco.pulse.outbox.infrastructure;

import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.outbox.api.ProcessedEventPort;
import ru.agroeco.pulse.outbox.domain.ProcessedEvent;
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
        if (repository.existsByConsumerNameAndEventId(consumerName, eventId)) {
            return false;
        }

        try {
            repository.save(new ProcessedEvent(consumerName, eventId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}
