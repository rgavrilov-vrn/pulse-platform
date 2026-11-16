package ru.agroeco.pulse.trassir.application.dto;

import java.util.Map;

public record TrassirSystemEventStatsView(
        long totalCount,
        Map<String, Long> bySeverity,
        Map<String, Long> byEventType,
        Map<String, Long> byObjectId
) {}