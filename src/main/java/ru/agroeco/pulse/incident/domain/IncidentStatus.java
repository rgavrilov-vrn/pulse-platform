package ru.agroeco.pulse.incident.domain;

public enum IncidentStatus {
    DRAFT, RECORDED, NEEDS_REVIEW, CONFIRMED, REJECTED, CLOSED, CANCELLED, ARCHIVED;

    /**
     * FIX #51: терминальный — только ARCHIVED. Раньше REJECTED/CANCELLED
     * считались терминальными, но seed status_transitions содержит
     * REJECTED→DRAFT, REJECTED→CANCELLED, CANCELLED→ARCHIVED — противоречие
     * «Java блокирует, БД разрешает». Теперь единственный источник правды —
     * матрица переходов; из ARCHIVED переходов нет вообще.
     */
    public boolean isTerminal() {
        return this == ARCHIVED;
    }

    /**
     * FIX #54: редактирование содержимого (категория, eventTime, checkpoint)
     * разрешено только в этих статусах. CONFIRMED/CLOSED — уже зафиксированы,
     * REJECTED/CANCELLED — вне активной работы, ARCHIVED — терминальный.
     */
    public boolean allowsContentEdit() {
        return this == DRAFT || this == RECORDED || this == NEEDS_REVIEW;
    }

    /** Изменение severity / площадки — только в черновике. */
    public boolean allowsDraftOnlyEdit() {
        return this == DRAFT;
    }
}