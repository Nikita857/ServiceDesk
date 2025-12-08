package com.bm.wschat.feature.message.repository;

import com.bm.wschat.feature.message.model.Message;
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
public interface MessageRepository extends JpaRepository<Message, Long> {

    // Все сообщения тикета (для специалистов)
    Page<Message> findByTicketIdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);

    // Только публичные сообщения (для пользователей)
    Page<Message> findByTicketIdAndInternalFalseOrderByCreatedAtDesc(Long ticketId, Pageable pageable);

    // Последние N сообщений (для чата)
    List<Message> findTop50ByTicketIdOrderByCreatedAtDesc(Long ticketId);

    // Количество непрочитанных для пользователя
    @Query("SELECT COUNT(m) FROM Message m WHERE m.ticket.id = :ticketId " +
            "AND m.sender.id != :userId AND m.readByUserAt IS NULL AND m.internal = false")
    Long countUnreadByTicketForUser(@Param("ticketId") Long ticketId, @Param("userId") Long userId);

    // Количество непрочитанных для специалиста
    @Query("SELECT COUNT(m) FROM Message m WHERE m.ticket.id = :ticketId " +
            "AND m.sender.id != :userId AND m.readBySpecialistAt IS NULL")
    Long countUnreadByTicketForSpecialist(@Param("ticketId") Long ticketId, @Param("userId") Long userId);

    // Пометить прочитанными для пользователя
    @Modifying
    @Query("UPDATE Message m SET m.readByUserAt = :now WHERE m.ticket.id = :ticketId " +
            "AND m.readByUserAt IS NULL AND m.sender.id != :userId")
    int markAsReadByUser(@Param("ticketId") Long ticketId, @Param("userId") Long userId, @Param("now") Instant now);

    // Пометить прочитанными для специалиста
    @Modifying
    @Query("UPDATE Message m SET m.readBySpecialistAt = :now WHERE m.ticket.id = :ticketId " +
            "AND m.readBySpecialistAt IS NULL AND m.sender.id != :userId")
    int markAsReadBySpecialist(@Param("ticketId") Long ticketId, @Param("userId") Long userId,
            @Param("now") Instant now);

    // Общее количество сообщений в тикете
    Long countByTicketId(Long ticketId);

    // Количество публичных сообщений
    Long countByTicketIdAndInternalFalse(Long ticketId);
}
