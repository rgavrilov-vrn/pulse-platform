package ru.agroeco.pulse.goodwan.ingestion;

public record GoodWanDataHistoryIngestResult(
    String deviceId,
    int receivedCount,
    int savedCount,
    long maxSourceRecordId,
    boolean hasMore
) {
}
