package ru.agroeco.pulse.trassir.application;

import java.util.Map;

public class TrassirEventRouter {

    private final Map<String, TrassirEventUseCase> handlers;

    public TrassirEventRouter(Map<String, TrassirEventUseCase> handlers) {
        this.handlers = Map.copyOf(handlers);
    }

    public EventProcessingResult route(TrassirEventContext context) {
        TrassirEventUseCase handler = handlers.get(context.event().eventType());

        if (handler == null) {
            return EventProcessingResult.success();
        }

        return handler.handle(context);
    }
}
