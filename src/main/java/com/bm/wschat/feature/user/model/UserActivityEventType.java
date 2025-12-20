package com.bm.wschat.feature.user.model;

/**
 * Типы событий активности пользователя для аудита.
 */
public enum UserActivityEventType {
    /** Вход в систему */
    LOGIN,
    /** Выход из системы */
    LOGOUT,
    /** Ручная смена статуса активности */
    STATUS_CHANGED
}
