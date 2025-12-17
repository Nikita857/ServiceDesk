# WebSocket маршруты (Ticket)

## Каналы подписки (Subscribe)

| Канал                         | Описание           | Данные                        |
| ----------------------------- | ------------------ | ----------------------------- |
| `/topic/ticket/new`           | Новый тикет создан | `TicketResponse`              |
| `/topic/ticket/{id}`          | Обновление тикета  | `TicketResponse`              |
| `/topic/ticket/{id}/messages` | Сообщения чата     | `ChatMessage`                 |
| `/topic/ticket/{id}/typing`   | Индикатор печати   | `TypingIndicator`             |
| `/topic/ticket/{id}/deleted`  | Тикет удалён       | `{ id: Long, deleted: true }` |

---

## Каналы отправки (App)

| Канал                     | Описание            | Данные               |
| ------------------------- | ------------------- | -------------------- |
| `/app/ticket/{id}/send`   | Отправить сообщение | `SendMessageRequest` |
| `/app/ticket/{id}/typing` | Печатает...         | `TypingIndicator`    |

---

## Методы, отправляющие события

### TicketService

| Метод                | Канал                        |
| -------------------- | ---------------------------- |
| `createTicket`       | `/topic/ticket/new`          |
| `updateTicket`       | `/topic/ticket/{id}`         |
| `changeStatus`       | `/topic/ticket/{id}`         |
| `takeTicket`         | `/topic/ticket/{id}`         |
| `assignToLine`       | `/topic/ticket/{id}`         |
| `assignToSpecialist` | `/topic/ticket/{id}`         |
| `setUserCategory`    | `/topic/ticket/{id}`         |
| `setSupportCategory` | `/topic/ticket/{id}`         |
| `deleteTicket`       | `/topic/ticket/{id}/deleted` |

### AssignmentService

| Метод              | Канал                |
| ------------------ | -------------------- |
| `createAssignment` | `/topic/ticket/{id}` |
| `acceptAssignment` | `/topic/ticket/{id}` |
| `rejectAssignment` | `/topic/ticket/{id}` |

### MessageWebSocketController

| Метод                 | Канал                         |
| --------------------- | ----------------------------- |
| `sendMessage`         | `/topic/ticket/{id}/messages` |
| `sendTypingIndicator` | `/topic/ticket/{id}/typing`   |

---

## Пример подписки (JavaScript)

```javascript
// STOMP клиент
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

    // Подписка на индикатор печати
    client.subscribe("/topic/ticket/123/typing", (message) => {
      const typing = JSON.parse(message.body);
      console.log("Печатает:", typing);
    });

    // Подписка на удаление тикета
    client.subscribe("/topic/ticket/123/deleted", (message) => {
      const data = JSON.parse(message.body);
      console.log("Тикет удалён:", data.id);
    });
  },
});

client.activate();
```
