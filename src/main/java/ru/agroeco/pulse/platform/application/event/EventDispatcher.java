package ru.agroeco.pulse.platform.application.event;

import ru.agroeco.pulse.platform.domain.event.DomainEvent;

public interface EventDispatcher {
    void dispatch(DomainEvent event);
}
