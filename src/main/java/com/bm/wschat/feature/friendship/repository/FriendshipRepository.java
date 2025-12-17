package com.bm.wschat.feature.friendship.repository;

import com.bm.wschat.feature.friendship.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /**
     * Найти запрос дружбы между двумя пользователями (в любом направлении)
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :userId1 AND f.addressee.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.addressee.id = :userId1)")
    Optional<Friendship> findBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Найти конкретный запрос (от requester к addressee)
     */
    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    /**
     * Входящие запросы в друзья (ожидающие)
     */
    @Query("SELECT f FROM Friendship f WHERE f.addressee.id = :userId AND f.status = 'PENDING' ORDER BY f.requestedAt DESC")
    List<Friendship> findPendingRequestsForUser(@Param("userId") Long userId);

    /**
     * Исходящие запросы в друзья (ожидающие)
     */
    @Query("SELECT f FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'PENDING' ORDER BY f.requestedAt DESC")
    List<Friendship> findOutgoingRequestsForUser(@Param("userId") Long userId);

    /**
     * Список друзей (принятые запросы в обе стороны)
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester.id = :userId OR f.addressee.id = :userId) AND " +
            "f.status = 'ACCEPTED' ORDER BY f.respondedAt DESC")
    List<Friendship> findFriendsOfUser(@Param("userId") Long userId);

    /**
     * Проверка дружбы между пользователями
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "((f.requester.id = :userId1 AND f.addressee.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.addressee.id = :userId1)) AND " +
            "f.status = 'ACCEPTED'")
    boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * Количество входящих запросов
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.addressee.id = :userId AND f.status = 'PENDING'")
    long countPendingRequests(@Param("userId") Long userId);

    /**
     * Проверка блокировки
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "((f.requester.id = :userId1 AND f.addressee.id = :userId2) OR " +
            "(f.requester.id = :userId2 AND f.addressee.id = :userId1)) AND " +
            "f.status = 'BLOCKED'")
    boolean isBlockedBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
