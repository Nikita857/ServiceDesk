package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.TicketStatusHistory;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для истории статусов тикетов.
 */
@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    /**
     * Найти всю историю статусов тикета.
     */
    List<TicketStatusHistory> findByTicketIdOrderByEnteredAtAsc(Long ticketId);

    /**
     * Найти текущий (незакрытый) статус тикета.
     */
    Optional<TicketStatusHistory> findByTicketIdAndExitedAtIsNull(Long ticketId);

    /**
     * Получить суммарное время в конкретном статусе для тикета.
     */
    @Query("SELECT COALESCE(SUM(h.durationSeconds), 0) FROM TicketStatusHistory h " +
            "WHERE h.ticket.id = :ticketId AND h.status = :status")
    Long getTotalDurationInStatus(Long ticketId, TicketStatus status);

    /**
     * Получить суммарное время во всех активных статусах (OPEN, PENDING,
     * ESCALATED).
     */
    @Query("SELECT COALESCE(SUM(h.durationSeconds), 0) FROM TicketStatusHistory h " +
            "WHERE h.ticket.id = :ticketId AND h.status IN ('OPEN', 'PENDING', 'ESCALATED')")
    Long getTotalActiveTime(Long ticketId);

    /**
     * Проверить, был ли тикет когда-либо в статусе.
     */
    boolean existsByTicketIdAndStatus(Long ticketId, TicketStatus status);

    // =====================================================================
    // REPORT QUERIES
    // =====================================================================

    /**
     * Суммарное время работы специалиста за период (по всем активным статусам).
     */
    @Query("SELECT COALESCE(SUM(h.durationSeconds), 0) FROM TicketStatusHistory h " +
            "WHERE h.changedBy.id = :specialistId " +
            "AND h.enteredAt >= :from AND h.enteredAt < :to " +
            "AND h.status IN (com.bm.wschat.feature.ticket.model.TicketStatus.OPEN, " +
            "                 com.bm.wschat.feature.ticket.model.TicketStatus.PENDING, " +
            "                 com.bm.wschat.feature.ticket.model.TicketStatus.ESCALATED)")
    Long sumDurationBySpecialistAndPeriod(Long specialistId, Instant from, Instant to);

    /**
     * Количество уникальных тикетов, над которыми работал специалист за период.
     */
    @Query("SELECT COUNT(DISTINCT h.ticket.id) FROM TicketStatusHistory h " +
            "WHERE h.changedBy.id = :specialistId " +
            "AND h.enteredAt >= :from AND h.enteredAt < :to")
    Long countDistinctTicketsBySpecialistAndPeriod(Long specialistId, Instant from, Instant to);

    /**
     * Суммарное время работы по линии поддержки за период.
     */
    @Query("SELECT COALESCE(SUM(h.durationSeconds), 0) FROM TicketStatusHistory h " +
            "WHERE h.ticket.supportLine.id = :lineId " +
            "AND h.enteredAt >= :from AND h.enteredAt < :to " +
            "AND h.status IN (com.bm.wschat.feature.ticket.model.TicketStatus.OPEN, " +
            "                 com.bm.wschat.feature.ticket.model.TicketStatus.PENDING, " +
            "                 com.bm.wschat.feature.ticket.model.TicketStatus.ESCALATED)")
    Long sumDurationByLineAndPeriod(Long lineId, Instant from, Instant to);

    /**
     * Количество уникальных тикетов по линии поддержки за период.
     */
    @Query("SELECT COUNT(DISTINCT h.ticket.id) FROM TicketStatusHistory h " +
            "WHERE h.ticket.supportLine.id = :lineId " +
            "AND h.enteredAt >= :from AND h.enteredAt < :to")
    Long countDistinctTicketsByLineAndPeriod(Long lineId, Instant from, Instant to);
}
