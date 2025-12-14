package com.bm.wschat.feature.friendship.model;

/**
 * Статусы дружбы между пользователями
 */
public enum FriendshipStatus {
    PENDING, // ожидает ответа
    ACCEPTED, // друзья
    REJECTED, // отклонен
    BLOCKED // заблокирован
}
