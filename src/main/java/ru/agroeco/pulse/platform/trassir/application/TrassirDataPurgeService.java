package ru.agroeco.pulse.platform.trassir.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.trassir.repository.TrassirEventRepository;
import ru.agroeco.pulse.platform.trassir.repository.TrassirIngestRawRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrassirDataPurgeService {

    private final TrassirEventRepository eventRepository;
    private final TrassirIngestRawRepository rawRepository;

    @Transactional
    public Map<String, Object> purgeAll() {
        int deletedEvents = eventRepository.deleteAllEvents();
        int deletedRaw = rawRepository.deleteAllRaw();

        log.warn("TRASSIR admin purge: deleted {} events and {} raw batches", deletedEvents, deletedRaw);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deletedEvents", deletedEvents);
        result.put("deletedRawBatches", deletedRaw);
        return result;
    }
}
