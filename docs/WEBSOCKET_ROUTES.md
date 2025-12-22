# WebSocket маршруты (Ticket)

## Архитектура

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────┐      ┌──────────────┐
│  TicketService  │─────▶│ TicketEventPub   │─────▶│  RabbitMQ   │─────▶│ EventConsumer│
│ AttachmentSvc   │      │   (Producer)     │      │    Queue    │      │  (WebSocket) │
│ MessageWSCtrl   │      └──────────────────┘      └─────────────┘      └──────┬───────┘
└─────────────────┘                                                            │
                                                                               ▼
                                                                    ┌──────────────────┐
                                                                    │  Frontend Client │
                                                                    │   (WebSocket)    │
                                                                    └──────────────────┘
```

---

## Каналы подписки (Subscribe)

| Канал                                | Описание                 | Данные                        |
| ------------------------------------ | ------------------------ | ----------------------------- |
| `/topic/ticket/new`                  | Новый тикет создан       | `TicketResponse`              |
| `/topic/ticket/{id}`                 | Обновление тикета        | `TicketResponse`              |
| `/topic/ticket/{id}/messages`        | Сообщения чата           | `ChatMessage`                 |
| `/topic/ticket/{id}/typing`          | Индикатор печати         | `TypingIndicator`             |
| `/topic/ticket/{id}/deleted`         | Тикет удалён             | `{ id: Long, deleted: true }` |
| `/topic/ticket/{id}/attachments`     | Вложение добавлено       | `AttachmentResponse`          |
| `/topic/ticket/{id}/internal`        | Внутренний комментарий   | `ChatMessage`                 |
| `/topic/sla/breach`                  | SLA нарушен              | `TicketResponse`              |
| `/topic/user/{userId}/notifications` | Уведомления пользователю | `Notification`                |

---

## Каналы отправки (App)

| Канал                     | Описание            | Данные               |
| ------------------------- | ------------------- | -------------------- |
| `/app/ticket/{id}/send`   | Отправить сообщение | `SendMessageRequest` |
| `/app/ticket/{id}/typing` | Печатает...         | `TypingIndicator`    |

---

## RabbitMQ Events

| Тип события        | Producer                               | Описание               |
| ------------------ | -------------------------------------- | ---------------------- |
| `CREATED`          | TicketService.createTicket             | Тикет создан           |
| `UPDATED`          | TicketService.updateTicket             | Тикет обновлён         |
| `STATUS_CHANGED`   | TicketService.changeStatus             | Статус изменён         |
| `ASSIGNED`         | TicketService.takeTicket, assignToLine | Назначение             |
| `RATED`            | TicketService.rateTicket               | Оценка поставлена      |
| `DELETED`          | TicketService.deleteTicket             | Тикет удалён           |
| `MESSAGE_SENT`     | MessageWebSocketController             | Новое сообщение        |
| `ATTACHMENT_ADDED` | AttachmentService                      | Вложение добавлено     |
| `INTERNAL_COMMENT` | (reserved)                             | Внутренний комментарий |
| `SLA_BREACH`       | (reserved)                             | SLA нарушен            |

---

## Методы, публикующие события через RabbitMQ

### TicketService

| Метод          | Событие RabbitMQ | WebSocket канал              |
| -------------- | ---------------- | ---------------------------- |
| `createTicket` | CREATED          | `/topic/ticket/new`          |
| `updateTicket` | UPDATED          | `/topic/ticket/{id}`         |
| `changeStatus` | STATUS_CHANGED   | `/topic/ticket/{id}`         |
| `takeTicket`   | ASSIGNED         | `/topic/ticket/{id}`         |
| `rateTicket`   | RATED            | `/topic/ticket/{id}`         |
| `assignToLine` | ASSIGNED         | `/topic/ticket/{id}`         |
| `deleteTicket` | DELETED          | `/topic/ticket/{id}/deleted` |

### AttachmentService

| Метод             | Событие RabbitMQ | WebSocket канал                  |
| ----------------- | ---------------- | -------------------------------- |
| `uploadToTicket`  | ATTACHMENT_ADDED | `/topic/ticket/{id}/attachments` |
| `uploadToMessage` | ATTACHMENT_ADDED | `/topic/ticket/{id}/attachments` |

### MessageWebSocketController

| Метод                    | Событие RabbitMQ | WebSocket канал               |
| ------------------------ | ---------------- | ----------------------------- |
| `sendMessage` (public)   | MESSAGE_SENT     | `/topic/ticket/{id}/messages` |
| `sendMessage` (internal) | — (direct WS)    | `/queue/ticket/{id}`          |
| `sendTypingIndicator`    | — (direct WS)    | `/topic/ticket/{id}/typing`   |

---

## Конфигурация RabbitMQ

```yaml
# application.yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

| Параметр    | Значение                     |
| ----------- | ---------------------------- |
| Exchange    | `servicedesk.events` (Topic) |
| Queue       | `servicedesk.ticket.events`  |
| Routing Key | `ticket.#`                   |

---

## Пример подписки (JavaScript)

```javascript
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "ws://localhost:8080/ws",
  onConnect: () => {
    // Подписка на новые тикеты
    client.subscribe("/topic/ticket/new", (message) => {
      const ticket = JSON.parse(message.body);
      console.log("Новый тикет:", ticket);
    });

    // Подписка на обновления конкретного тикета
    client.subscribe("/topic/ticket/123", (message) => {
      const ticket = JSON.parse(message.body);
      console.log("Тикет обновлён:", ticket);
    });

    // Подписка на сообщения чата тикета
    client.subscribe("/topic/ticket/123/messages", (message) => {
      const chatMessage = JSON.parse(message.body);
      console.log("Новое сообщение:", chatMessage);
    });

    // Подписка на вложения
    client.subscribe("/topic/ticket/123/attachments", (message) => {
      const attachment = JSON.parse(message.body);
      console.log("Новое вложение:", attachment);
    });
  },
});

client.activate();
```
