package com.bm.wschat.feature.dm.repository;

import com.bm.wschat.feature.dm.model.DirectMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    /**
     * Получить переписку между двумя пользователями
     */
    @Query("SELECT dm FROM DirectMessage dm " +
            "WHERE (dm.sender.id = :userId1 AND dm.recipient.id = :userId2) " +
            "   OR (dm.sender.id = :userId2 AND dm.recipient.id = :userId1) " +
            "ORDER BY dm.createdAt DESC")
    Page<DirectMessage> findConversation(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable);

    /**
     * Количество непрочитанных от конкретного пользователя
     */
    @Query("SELECT COUNT(dm) FROM DirectMessage dm " +
            "WHERE dm.sender.id = :senderId AND dm.recipient.id = :recipientId AND dm.readAt IS NULL")
    Long countUnreadFrom(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

    /**
     * Общее количество непрочитанных для пользователя
     */
    @Query("SELECT COUNT(dm) FROM DirectMessage dm " +
            "WHERE dm.recipient.id = :userId AND dm.readAt IS NULL")
    Long countTotalUnread(@Param("userId") Long userId);

    /**
     * Пометить все сообщения от пользователя как прочитанные
     */
    @Modifying
    @Query("UPDATE DirectMessage dm SET dm.readAt = :now " +
            "WHERE dm.sender.id = :senderId AND dm.recipient.id = :recipientId AND dm.readAt IS NULL")
    int markAsReadFrom(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId,
            @Param("now") Instant now);

    /**
     * Список диалогов (последнее сообщение с каждым пользователем)
     */
    @Query(value = """
            SELECT DISTINCT ON (partner_id) *
            FROM (
                SELECT dm.*,
                       CASE WHEN dm.sender_id = :userId THEN dm.recipient_id ELSE dm.sender_id END as partner_id
                FROM direct_messages dm
                WHERE (dm.sender_id = :userId OR dm.recipient_id = :userId) AND dm.deleted_at IS NULL
            ) sub
            ORDER BY partner_id, created_at DESC
            """, nativeQuery = true)
    List<DirectMessage> findLatestInEachConversation(@Param("userId") Long userId);
}
