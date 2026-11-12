package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirEventRepository;
import ru.agroeco.pulse.trassir.adapter.persistence.TrassirIngestRawRepository;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirRetentionCleanupJob {

    private static final int BATCH_SIZE = 1000;

    private final TrassirIngestRawRepository rawRepository;
    private final TrassirEventRepository eventRepository;
    private final TrassirRetentionBatch batch;

    @Value("${pulse.trassir.retention.raw-days:30}")
    private long rawRetentionDays;

    @Value("${pulse.trassir.retention.finished-events-days:90}")
    private long finishedEventsRetentionDays;

    @Scheduled(cron = "${pulse.trassir.retention.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        Instant rawCutoff = Instant.now().minus(Duration.ofDays(rawRetentionDays));
        Instant eventCutoff = Instant.now().minus(Duration.ofDays(finishedEventsRetentionDays));

        long totalEvents = 0;
        long totalRaw = 0;
        int eventBatch;
        int rawBatch;
        do {
            eventBatch = batch.deleteEventBatch(eventCutoff, BATCH_SIZE);
            totalEvents += eventBatch;
        } while (eventBatch == BATCH_SIZE);

        do {
            rawBatch = batch.deleteRawBatch(rawCutoff, BATCH_SIZE);
            totalRaw += rawBatch;
        } while (rawBatch == BATCH_SIZE);

        log.info("TRASSIR retention cleanup: deleted {} events and {} raw batches",
                totalEvents, totalRaw);
    }
}