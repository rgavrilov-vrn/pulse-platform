package ru.agroeco.pulse.goodwan.adapter.http.dto;

public record GoodWanDataHistoryIngestResult(
    String deviceId,
    int receivedCount,
    int savedCount,
    long maxSourceRecordId,
    boolean hasMore
) {
}
