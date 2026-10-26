package ru.agroeco.pulse.goodwan.ingestion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.goodwan.config.GoodWanProperties;

@Component
@ConditionalOnProperty(
    prefix = "goodwan.sync",
    name = "enabled",
    havingValue = "true"
)
public class GoodWanDataHistorySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(
        GoodWanDataHistorySyncScheduler.class
    );

    private final GoodWanDataHistoryIngestionService ingestionService;
    private final GoodWanProperties properties;

    public GoodWanDataHistorySyncScheduler(
        GoodWanDataHistoryIngestionService ingestionService,
        GoodWanProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{T(java.time.Duration).parse('${goodwan.sync.interval:PT1M}').toMillis()}")
    public void sync() {
        List<String> deviceIds = properties.devices();

        for (String deviceId : deviceIds) {
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
