package ru.agroeco.pulse.videoevents.application.event;

import ru.agroeco.pulse.videoevents.domain.event.DomainEvent;

public interface EventDispatcher {
    void dispatch(DomainEvent event);
}
