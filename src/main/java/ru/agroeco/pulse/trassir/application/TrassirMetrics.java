package ru.agroeco.pulse.trassir.application;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

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

    /**
     * FIX #80: bounded error-тег. Раньше передавалось полное сообщение
     * исключения → тысячи уникальных time-series (кардинальный взрыв).
     * Классификация выполняется в вызывающем коде (TrassirEventProcessor.classify).
     */
    public void incrementEventFailed(String eventType, String errorCategory) {
        registry.counter("trassir_events_failed_total",
                "event_type", eventType,
                "error", errorCategory).increment();
    }

    public void incrementEventRetried(String eventType) {
        registry.counter("trassir_events_retried_total", "event_type", eventType).increment();
    }

    public void incrementEventDeadLetter(String eventType) {
        registry.counter("trassir_events_dead_letter_total", "event_type", eventType).increment();
    }

    /** FIX #81: событие неизвестного типа — раньше молча помечалось PROCESSED. */
    public void incrementEventUnknown(String eventType) {
        // eventType в теге — потенциально unbounded, но список типов
        // контролируется агентом и редко меняется.
        registry.counter("trassir_events_unknown_total", "event_type", eventType).increment();
    }

    public void recordEventProcessing(String eventType, long durationNanos) {
        registry.timer("trassir_event_processing_duration_seconds",
                "event_type", eventType).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordPendingSnapshots(long count) {
        registry.gauge("trassir_snapshots_pending_total", count);
    }

    public void incrementSnapshotRetried() {
        registry.counter("trassir_snapshots_retried_total").increment();
    }

    public void incrementSnapshotDeadLetter() {
        registry.counter("trassir_snapshots_dead_letter_total").increment();
    }
}