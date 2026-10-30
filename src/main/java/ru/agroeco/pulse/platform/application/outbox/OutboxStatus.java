package ru.agroeco.pulse.platform.application.outbox;

/**
 * Статусы outbox-событий.
 *
 * ВАЖНО: значения должны совпадать с CHECK-констрейнтом
 * 005_create_schema_outbox.sql:
 *   chk_outbox_events_status CHECK (status IN ('NEW','PUBLISHED','FAILED','DEAD'))
 *
 * В прошлой версии был {NEW, SENT, FAILED} — SENT вызывал CHECK violation
 * при каждом успешном send, DEAD из БД не читался.
 */
public enum OutboxStatus {
    NEW,        // ожидает публикации
    PUBLISHED,  // успешно опубликовано в Kafka
    FAILED,     // временная ошибка, будет retry
    DEAD        // исчерпаны попытки, требуется ручной retry
}