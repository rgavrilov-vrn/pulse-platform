# TRASSIR telemetry contract

## Endpoint

`POST /api/v1/trassir/ingest`

- Content-Type: `application/json`
- Body: один JSON-конверт `trassir.telemetry`
- Успех: HTTP 200, пустое тело
- Ошибка контракта: HTTP 400
- Нет JWT: HTTP 401
- Нет права `trassir:ingest`: HTTP 403

## Envelope

```json
{
  "contract": "trassir.telemetry",
  "contract_version": "1.0",
  "batch_id": "UUID",
  "sent_at": "ISO-8601 UTC",
  "producer": {
    "name": "trassir-sdk-agent",
    "version": "1.0.0"
  },
  "server": {
    "server_id": "video81"
  },
  "events": []
}
```

## Event types

- `agent.heartbeat`
- `agent.objects_snapshot`
- `trassir.system_event`

Неизвестный `event_type` сохраняется в `trassir.events` и не маршрутизируется в специализированный use case.

## Storage rules

- `trassir.ingest_raw` хранит исходный конверт без изменений.
- `trassir.events` хранит каждое событие с исходным JSON.
- `trassir.events.processing_error` — единственное обновляемое бизнес-поле события.
- Snapshot-части собираются в `trassir.snapshot_parts` и применяются после получения последней части.
- `trassir.objects` — текущее состояние объектов; удаление логическое через `deleted_at`.
- `trassir.system_events` — append-only проекция системных событий.
