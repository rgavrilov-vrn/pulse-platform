package ru.agroeco.pulse.goodwan.ingestion;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.agroeco.pulse.goodwan.config.GoodWanProperties;

@Component
public class GoodWanIngestionManager {

    private static final Logger log = LoggerFactory.getLogger(
        GoodWanIngestionManager.class
    );

    private final GoodWanDataHistoryIngestionService ingestionService;
    private final GoodWanProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public GoodWanIngestionManager(
        GoodWanDataHistoryIngestionService ingestionService,
        GoodWanProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString =
            "#{T(java.time.Duration).parse('${goodwan.sync.interval:PT1M}').toMillis()}"
    )
    public void scheduledSync() {
        if (!properties.sync().enabled()) {
            return;
        }

        start();
    }

    public GoodWanSchedulerState start() {
        if (running.compareAndSet(false, true)) {
            log.info("GoodWAN data history ingestion started");

            try {
                ingestAllDevices();
            } finally {
                running.set(false);
                log.info("GoodWAN data history ingestion finished");
            }
        } else {
            log.info("GoodWAN data history ingestion is already running");
        }

        return currentState();
    }

    public GoodWanSchedulerState stop() {
        running.set(false);

        log.info("GoodWAN data history ingestion stop requested");

        return currentState();
    }

    public GoodWanSchedulerState currentState() {
        return running.get()
            ? GoodWanSchedulerState.RUNNING
            : GoodWanSchedulerState.STOPPED;
    }

    private void ingestAllDevices() {
        List<String> deviceIds = properties.devices();

        for (String deviceId : deviceIds) {
            if (!running.get()) {
                log.info(
                    "GoodWAN data history ingestion stopped before device {}",
                    deviceId
                );
                return;
            }

            try {
                long fromId = 0;

                var result = ingestionService.ingest(deviceId, fromId);

                log.info(
                    "GoodWAN data history sync finished: deviceId={}, received={}, saved={}, maxId={}, hasMore={}",
                    result.deviceId(),
                    result.receivedCount(),
                    result.savedCount(),
                    result.maxSourceRecordId(),
                    result.hasMore()
                );
            } catch (Exception e) {
                log.error(
                    "GoodWAN data history sync failed: deviceId={}",
                    deviceId,
                    e
                );
            }
        }
    }
}
