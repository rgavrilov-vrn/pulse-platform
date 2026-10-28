package ru.agroeco.pulse.outbox.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.agroeco.pulse.outbox.domain.ProcessedEvent;

public interface ProcessedEventRepository
    extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);
}
