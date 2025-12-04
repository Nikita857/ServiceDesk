package com.bm.wschat.feature.ticket.model;

public enum TicketStatus {
    NEW, // только создан
    OPEN, // в работе
    PENDING, // ждем ответ от пользователя
    ESCALATED, // передан на другую линию
    RESOLVED, // проблема решена
    CLOSED, // закрыт
    REOPENED, // открыт снова
    REJECTED, // отклонен
    CANCELLED // отменен пользователем
}
