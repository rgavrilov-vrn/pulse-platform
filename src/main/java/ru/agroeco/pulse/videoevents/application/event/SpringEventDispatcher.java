package ru.agroeco.pulse.videoevents.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.videoevents.domain.event.DomainEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventDispatcher implements EventDispatcher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void dispatch(DomainEvent event) {
        log.debug("Dispatching domain event: type={}, incidentId={}", event.eventType(), event.entityId());
        publisher.publishEvent(event);
    }
}
