package com.bm.wschat.feature.ticket.repository;

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

    // Статистика по времени решения (resolvedAt - createdAt)
    @Query("SELECT COUNT(t), " +
            "AVG(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt))), " +
            "MIN(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt))), " +
            "MAX(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt))) " +
            "FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    List<Object[]> getResolutionTimeStats();

    // Количество решенных тикетов специалистом за период
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :specialistId AND t.resolvedAt BETWEEN :from AND :to")
    Long countResolvedBySpecialistAndPeriod(
            @Param("specialistId") Long specialistId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Среднее время решения для специалиста
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (t.resolvedAt - t.createdAt))) FROM Ticket t WHERE t.assignedTo.id = :specialistId AND t.resolvedAt IS NOT NULL")
    Double getAvgResolutionTimeBySpecialist(@Param("specialistId") Long specialistId);
}
