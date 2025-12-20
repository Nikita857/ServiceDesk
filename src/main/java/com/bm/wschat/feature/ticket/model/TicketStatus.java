package com.bm.wschat.feature.ticket.model;

/**
 * Статусы тикета в системе.
 * NEW → OPEN → PENDING → OPEN → RESOLVED → PENDING_CLOSURE → CLOSED
 * ↘ REJECTED ↘ REOPENED
 * ↘ CANCELLED ↓
 * OPEN
 * Двухфакторное закрытие:
 * - Специалист переводит тикет в PENDING_CLOSURE
 * - Пользователь подтверждает → CLOSED
 * - Пользователь отклоняет → REOPENED
 * - Администратор может закрыть принудительно → CLOSED
 */
public enum TicketStatus {
    /** Только создан, ожидает взятия в работу */
    NEW,

    /** В работе у специалиста */
    OPEN,

    /** Ожидание ответа от пользователя */
    PENDING,

    /** Передан на другую линию поддержки */
    ESCALATED,

    /** Проблема решена, ожидает подтверждения */
    RESOLVED,

    /** Ожидание подтверждения закрытия от пользователя (двухфакторное) */
    PENDING_CLOSURE,

    /** Закрыт (финальный статус) */
    CLOSED,

    /** Открыт повторно после закрытия или решения */
    REOPENED,

    /** Отклонён специалистом */
    REJECTED,

    /** Отменён пользователем */
    CANCELLED;

    /**
     * Проверяет, является ли статус финальным (тикет закрыт).
     */
    public boolean isFinal() {
        return this == CLOSED || this == REJECTED || this == CANCELLED;
    }

    /**
     * Проверяет, находится ли тикет в активной работе.
     */
    public boolean isActive() {
        return this == OPEN || this == PENDING || this == ESCALATED;
    }
}
