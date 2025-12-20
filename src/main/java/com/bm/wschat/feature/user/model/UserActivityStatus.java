package com.bm.wschat.feature.user.model;

/**
 * Статусы активности специалиста.
 * Влияет на возможность назначения тикетов.
 */
public enum UserActivityStatus {
    /** Доступен для работы */
    AVAILABLE,
    /** Недоступен (вручную установлен) */
    UNAVAILABLE,
    /** Занят, но может принимать тикеты (с предупреждением на UI) */
    BUSY,
    /** Технические неполадки */
    TECHNICAL_ISSUE,
    /** Оффлайн (автоматически при выходе) */
    OFFLINE;

    /**
     * Проверяет, может ли специалист с данным статусом получать тикеты.
     * AVAILABLE и BUSY - можно назначать.
     * UNAVAILABLE, TECHNICAL_ISSUE, OFFLINE - нельзя.
     */
    public boolean isAvailableForAssignment() {
        return this == AVAILABLE || this == BUSY;
    }
}
