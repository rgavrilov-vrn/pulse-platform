package ru.agroeco.pulse.incident.domain;

public enum IncidentKind {

    EVENT, VIOLATION;

    /**
     * FIX #148: nullable-вариант — не бросает IllegalArgumentException.
     * Раньше `deriveKind()` в IncidentService и `fromGroupCode()` в
     * IncidentSummaryEnricher давали 500 при неизвестной группе
     * (например, при добавлении новой группы в БД без изменения enum).
     */
    public static IncidentKind fromGroupCodeOrNull(String code) {
        if (code == null) return null;
        return switch (code) {
            case "EVENT"     -> EVENT;
            case "VIOLATION" -> VIOLATION;
            default          -> null;
        };
    }

    /**
     * @deprecated используйте {@link #fromGroupCodeOrNull(String)} в сервисах
     * для graceful degradation. Строгий вариант оставлен только для случаев,
     * где IllegalArgumentException действительно допустим (тесты, seed-валидация).
     */
    @Deprecated
    public static IncidentKind fromGroupCode(String code) {
        IncidentKind kind = fromGroupCodeOrNull(code);
        if (kind == null) {
            throw new IllegalArgumentException("Unknown group code: " + code);
        }
        return kind;
    }

    public String toGroupCode() { return name(); }
}