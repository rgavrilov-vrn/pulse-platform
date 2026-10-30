package ru.agroeco.pulse.goodwan.ingestion;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.agroeco.pulse.goodwan.api.GoodWanContractException;
import ru.agroeco.pulse.goodwan.api.GoodWanDataHistoryClient;
import ru.agroeco.pulse.goodwan.config.GoodWanProperties;
import ru.agroeco.pulse.goodwan.raw.RawDataHistoryEntity;
import ru.agroeco.pulse.goodwan.raw.RawDataHistoryItem;
import ru.agroeco.pulse.goodwan.raw.RawDataHistoryMapper;
import ru.agroeco.pulse.goodwan.raw.RawDataHistoryRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class GoodWanDataHistoryIngestionService {

    private static final String ENDPOINT = "/data/history";

    private final GoodWanDataHistoryClient client;
    private final RawDataHistoryRepository repository;
    private final RawDataHistoryMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int pageSize;

    public GoodWanDataHistoryIngestionService(
        GoodWanDataHistoryClient client,
        RawDataHistoryRepository repository,
        RawDataHistoryMapper mapper,
        ObjectMapper objectMapper,
        Clock clock,
        GoodWanProperties properties
    ) {
        this.client = client;
        this.repository = repository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.pageSize = properties.sync().pageSize();
    }

    @Transactional
    public GoodWanDataHistoryIngestResult ingest(
        String deviceId,
        long fromId
    ) {
        Instant requestedAt = Instant.now(clock);

        List<RawDataHistoryItem> items = client.fetchHistory(
            deviceId,
            fromId,
            pageSize
        );

        String requestParametersJson = buildRequestParameters(
            deviceId,
            fromId,
            pageSize
        );

        int savedCount = 0;
        long maxSourceRecordId = fromId;

        for (RawDataHistoryItem item : items) {
            Long sourceRecordId = item.sourceRecordId();

            if (sourceRecordId == null) {
                throw new GoodWanContractException(
                    "GoodWAN data history item has no numeric id"
                );
            }

            boolean exists = repository
                .existsByExternalDeviceIdAndSourceRecordId(
                    deviceId,
                    sourceRecordId
                );

            if (!exists) {
                RawDataHistoryEntity entity = mapper.toEntity(
                    deviceId,
                    item,
                    requestParametersJson,
                    requestedAt,
                    Instant.now(clock)
                );

                repository.save(entity);
                savedCount++;
            }

            maxSourceRecordId = Math.max(maxSourceRecordId, sourceRecordId);
        }

        return new GoodWanDataHistoryIngestResult(
            deviceId,
            items.size(),
            savedCount,
            maxSourceRecordId,
            items.size() == pageSize
        );
    }

    private String buildRequestParameters(
        String deviceId,
        long fromId,
        int count
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("deviceid", deviceId);
        parameters.put("fromId", fromId);
        parameters.put("count", count);

        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JacksonException e) {
            throw new GoodWanRawMappingException(
                "Cannot serialize GoodWAN request parameters",
                e
            );
        }
    }
}
