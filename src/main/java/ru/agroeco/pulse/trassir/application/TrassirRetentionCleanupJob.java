package ru.agroeco.pulse.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.trassir.repository.TrassirIngestRawRepository;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrassirRetentionCleanupJob {

    private final TrassirIngestRawRepository rawRepository;
    private final TrassirEventRepository eventRepository;

    @Value("${pulse.trassir.retention.raw-days:30}")
    private long rawRetentionDays;

    @Value("${pulse.trassir.retention.finished-events-days:90}")
    private long finishedEventsRetentionDays;

    @Scheduled(cron = "${pulse.trassir.retention.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant rawCutoff = Instant.now().minus(Duration.ofDays(rawRetentionDays));
        Instant eventCutoff = Instant.now().minus(Duration.ofDays(finishedEventsRetentionDays));

        int deletedEvents = eventRepository.deleteFinishedOlderThan(eventCutoff);
        int deletedRaw = rawRepository.deleteOlderThan(rawCutoff);

        log.info("TRASSIR retention cleanup: deleted {} events and {} raw batches", deletedEvents, deletedRaw);
    }
}
