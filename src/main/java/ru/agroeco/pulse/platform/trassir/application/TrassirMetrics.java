package ru.agroeco.pulse.platform.trassir.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TrassirMetrics {

    private final MeterRegistry registry;

    public TrassirMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementIngest(String result) {
        registry.counter("trassir_ingest_total", "result", result).increment();
    }

    public Timer.Sample startIngest() {
        return Timer.start(registry);
    }

    public void stopIngest(Timer.Sample sample) {
        sample.stop(registry.timer("trassir_ingest_duration_seconds"));
    }

    public void incrementEventProcessed(String eventType) {
        registry.counter("trassir_events_processed_total", "event_type", eventType).increment();
    }

    public void incrementEventFailed(String eventType, String error) {
        registry.counter(
                "trassir_events_failed_total",
                "event_type", eventType,
                "error", error
        ).increment();
    }

    public void incrementEventRetried(String eventType) {
        registry.counter("trassir_events_retried_total", "event_type", eventType).increment();
    }

    public void incrementEventDeadLetter(String eventType) {
        registry.counter("trassir_events_dead_letter_total", "event_type", eventType).increment();
    }

    public void recordEventProcessing(String eventType, long durationNanos) {
        registry.timer(
                "trassir_event_processing_duration_seconds",
                "event_type", eventType
        ).record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
