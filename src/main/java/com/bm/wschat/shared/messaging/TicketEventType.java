package com.bm.wschat.shared.messaging;

/**
 * Типы событий тикетов для RabbitMQ.
 */
public enum TicketEventType {
    /** Тикет создан */
    CREATED,
    /** Тикет обновлён */
    UPDATED,
    /** Статус изменён */
    STATUS_CHANGED,
    /** Назначен исполнитель */
    ASSIGNED,
    /** Новое сообщение */
    MESSAGE_SENT,
    /** Оценка поставлена */
    RATED,
    /** Тикет удалён */
    DELETED,
    /** Вложение добавлено */
    ATTACHMENT_ADDED,
    /** Внутренний комментарий */
    INTERNAL_COMMENT,
    /** SLA нарушен */
    SLA_BREACH,
    /** Статус пользователя изменён */
    USER_STATUS_CHANGED,
    /** Сообщение обновлено */
    MESSAGE_UPDATED
}
