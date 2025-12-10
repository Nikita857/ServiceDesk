package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

        Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

        Page<Ticket> findByCreatedById(Long userId, Pageable pageable);

        Page<Ticket> findByAssignedToId(Long specialistId, Pageable pageable);

        Page<Ticket> findBySupportLineId(Long lineId, Pageable pageable);

        @Query("SELECT t FROM Ticket t " +
                        "LEFT JOIN FETCH t.createdBy " +
                        "LEFT JOIN FETCH t.assignedTo " +
                        "LEFT JOIN FETCH t.supportLine " +
                        "LEFT JOIN FETCH t.categoryUser " +
                        "LEFT JOIN FETCH t.categorySupport " +
                        "WHERE t.id = :id")
        Optional<Ticket> findByIdWithDetails(@Param("id") Long id);

        @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
        List<Object[]> countByStatus();

        @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :specialistId AND t.status NOT IN ('CLOSED', 'RESOLVED')")
        Long countActiveByAssignedTo(@Param("specialistId") Long specialistId);

        // =====================================================================
        // REPORT QUERIES
        // =====================================================================

        // Статистика по категориям пользователей
        @Query("SELECT t.categoryUser.id, t.categoryUser.name, COUNT(t) FROM Ticket t WHERE t.categoryUser IS NOT NULL GROUP BY t.categoryUser.id, t.categoryUser.name ORDER BY COUNT(t) DESC")
        List<Object[]> countByCategoryUser();

        // Статистика по категориям поддержки
        @Query("SELECT t.categorySupport.id, t.categorySupport.name, COUNT(t) FROM Ticket t WHERE t.categorySupport IS NOT NULL GROUP BY t.categorySupport.id, t.categorySupport.name ORDER BY COUNT(t) DESC")
        List<Object[]> countByCategorySupport();

        // Статистика по времени решения (resolvedAt - createdAt) - Native query for
        // PostgreSQL
        @Query(value = """
                        SELECT COUNT(*),
                               AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))),
                               MIN(EXTRACT(EPOCH FROM (resolved_at - created_at))),
                               MAX(EXTRACT(EPOCH FROM (resolved_at - created_at)))
                        FROM tickets
                        WHERE resolved_at IS NOT NULL AND deleted_at IS NULL
                        """, nativeQuery = true)
        List<Object[]> getResolutionTimeStats();

        // Количество решенных тикетов специалистом за период
        @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :specialistId AND t.resolvedAt BETWEEN :from AND :to")
        Long countResolvedBySpecialistAndPeriod(
                        @Param("specialistId") Long specialistId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        // Среднее время решения для специалиста - Native query for PostgreSQL
        @Query(value = """
                        SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)))
                        FROM tickets
                        WHERE assigned_to_id = :specialistId AND resolved_at IS NOT NULL AND deleted_at IS NULL
                        """, nativeQuery = true)
        Double getAvgResolutionTimeBySpecialist(@Param("specialistId") Long specialistId);

        // =====================================================================
        // VISIBILITY QUERIES - Tickets visible based on user's support lines
        // =====================================================================

        /**
         * Find tickets visible to a specialist:
         * - Tickets assigned to their support lines (not assigned to specific person)
         * - Tickets assigned directly to them
         */
        @Query("SELECT t FROM Ticket t WHERE " +
                        "(t.supportLine IN :lines AND t.assignedTo IS NULL) " +
                        "OR t.assignedTo.id = :userId")
        Page<Ticket> findVisibleToSpecialist(
                        @Param("lines") List<SupportLine> lines,
                        @Param("userId") Long userId,
                        Pageable pageable);

        /**
         * Find all tickets in given support lines
         */
        @Query("SELECT t FROM Ticket t WHERE t.supportLine IN :lines")
        Page<Ticket> findBySupportLineIn(
                        @Param("lines") List<SupportLine> lines,
                        Pageable pageable);
}
