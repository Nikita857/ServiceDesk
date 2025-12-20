# ServiceDesk - История изменений

## Ветка: feat/time-reports (текущая)

### Автоматический учёт времени

- ✅ Добавлена миграция `V8__ticket_status_history.sql`
- ✅ Создана сущность `TicketStatusHistory` для хранения истории статусов
- ✅ Создан `TicketTimeTrackingService` для автоматической записи времени
- ✅ Интеграция в `TicketService` — время записывается при каждой смене статуса
- ✅ Добавлено поле `first_response_at` — время первой реакции

### Двухфакторное закрытие тикетов

- ✅ Добавлен статус `PENDING_CLOSURE` в `TicketStatus`
- ✅ Логика: RESOLVED → PENDING_CLOSURE → CLOSED (или REOPENED)
- ✅ Специалист запрашивает закрытие, пользователь подтверждает
- ✅ Администратор может закрыть принудительно
- ✅ Поля `closure_requested_by_id`, `closure_requested_at` в Ticket

### Оценка качества обслуживания

- ✅ Миграция `V9__ticket_rating.sql` — поле `rated_at`
- ✅ `RateTicketRequest` DTO (оценка 1-5, feedback)
- ✅ `TicketRatingResponse` DTO
- ✅ Метод `rateTicket()` в TicketService
- ✅ Эндпоинт `POST /api/v1/tickets/{id}/rate`
- ✅ Тип уведомления `RATING` + factory-метод `Notification.rating()`
- ✅ Только создатель может оценить закрытый тикет (один раз)
- ✅ Администратор может закрыть принудительно
- ✅ Поля `closure_requested_by_id`, `closure_requested_at` в Ticket

### Новые API-эндпоинты

- ✅ `GET /api/v1/tickets/{id}/status-history` — история статусов
- ✅ `POST /api/v1/tickets/{id}/confirm-closure` — подтвердить закрытие
- ✅ `POST /api/v1/tickets/{id}/reject-closure` — отклонить закрытие

### Удалено (ручной учёт времени)

- ❌ `TimeEntry.java`, `TimeEntryType.java`
- ❌ `TimeEntryRepository.java`, `TimeEntryService.java`
- ❌ `TimeEntryController.java`, `TimeEntryMapper.java`
- ❌ `CreateTimeEntryRequest.java`, `UpdateTimeEntryRequest.java`, `TimeEntryResponse.java`

### Переработано

- ✅ `ReportService.java` — теперь использует `TicketStatusHistoryRepository`
- ✅ `Ticket.java` — удалено поле `timeEntries`, добавлены поля для closure
- ✅ `TicketMapper.java` — обновлены маппинги

---

## Ветка: feat/new-business-logic (закоммичена)

### Правила переадресации тикетов

- ✅ Создан `ForwardingRulesService` с правилами:
  - SYSADMIN → 1CSUPPORT
  - 1CSUPPORT → SYSADMIN, DEV1C
  - DEV1C → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
  - DEVELOPER → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
  - ADMIN → любая линия
- ✅ Интеграция в `AssignmentService` — валидация при переадресации
- ✅ Эндпоинт `GET /api/v1/assignments/available-lines`

---

## Ветка: feature/1c-support-line (закоммичена)

### Новая роль 1CSUPPORT

- ✅ Добавлена роль `ONE_C_SUPPORT` в `SenderType` с алиасом "1CSUPPORT"
- ✅ Обновлён seeder `R__seed.sql`:
  - 4-я линия поддержки "Поддержка 1С"
  - 3 тестовых пользователя с ролью 1CSUPPORT
  - Новая категория "Поддержка 1С"
- ✅ Обновлены все `@PreAuthorize` аннотации с 1CSUPPORT
- ✅ Исправлен `Message.determineSenderType()` для корректного определения роли

---

## Ветка: feature/user-activity (закоммичена)

### Активность пользователей

- ✅ `UserActivityStatus` enum с методом `isAvailableForAssignment()`
- ✅ `UserActivityEventType` — типы событий (LOGIN, LOGOUT, STATUS_CHANGED)
- ✅ `UserActivityStatusService` — управление статусами
- ✅ `UserActivityLogService` — логирование событий
- ✅ Интеграция в `AssignmentService` — проверка доступности при назначении
- ✅ `UserStatusResponse` DTO с полями status, availableForAssignment, updatedAt
- ✅ Эндпоинты в `UserController`:
  - `GET /api/v1/users/status` — свой статус
  - `GET /api/v1/users/{userId}/status` — статус пользователя
  - `PATCH /api/v1/users/status` — изменить статус
- ✅ `SpecialistResponse` с полями activityStatus, availableForAssignment

---

## TODO (из tz.txt)

- [ ] Уведомления о тикетах (Telegram, WebSocket)
- [ ] Выбор линии поддержки пользователем при создании тикета
- [ ] Оценка качества обслуживания
- [ ] Дашборд статистики
- [ ] Интеграция с Telegram-ботом
