# WebSocket маршруты (Ticket)

## Каналы подписки (Subscribe)

| Канал                        | Описание           | Данные                        |
| ---------------------------- | ------------------ | ----------------------------- |
| `/topic/ticket/new`          | Новый тикет создан | `TicketResponse`              |
| `/topic/ticket/{id}`         | Обновление тикета  | `TicketResponse`              |
| `/topic/ticket/{id}/deleted` | Тикет удалён       | `{ id: Long, deleted: true }` |

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

    // Подписка на удаление тикета
    client.subscribe("/topic/ticket/123/deleted", (message) => {
      const data = JSON.parse(message.body);
      console.log("Тикет удалён:", data.id);
    });
  },
});

client.activate();
```
