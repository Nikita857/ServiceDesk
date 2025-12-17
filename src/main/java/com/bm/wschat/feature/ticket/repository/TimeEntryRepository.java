package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.TimeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

        // Записи тикета
        List<TimeEntry> findByTicketIdOrderByEntryDateDesc(Long ticketId);

        Page<TimeEntry> findByTicketIdOrderByEntryDateDesc(Long ticketId, Pageable pageable);

        // Записи специалиста
        List<TimeEntry> findBySpecialistIdOrderByEntryDateDesc(Long specialistId);

        Page<TimeEntry> findBySpecialistIdOrderByEntryDateDesc(Long specialistId, Pageable pageable);

        // Итого по тикету
        @Query("SELECT COALESCE(SUM(t.durationSeconds), 0) FROM TimeEntry t WHERE t.ticket.id = :ticketId")
        Long sumDurationByTicketId(@Param("ticketId") Long ticketId);

        // Записи специалиста за период
        @Query("SELECT t FROM TimeEntry t WHERE t.specialist.id = :userId AND t.entryDate BETWEEN :from AND :to ORDER BY t.entryDate DESC")
        List<TimeEntry> findBySpecialistIdAndPeriod(
                        @Param("userId") Long userId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        // Записи специалиста за дату
        List<TimeEntry> findBySpecialistIdAndWorkDateOrderByEntryDateDesc(Long specialistId, LocalDate workDate);

        // Итого по специалисту за период
        @Query("SELECT COALESCE(SUM(t.durationSeconds), 0) FROM TimeEntry t WHERE t.specialist.id = :userId AND t.entryDate BETWEEN :from AND :to")
        Long sumDurationBySpecialistAndPeriod(
                        @Param("userId") Long userId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        // Количество записей по тикету
        Long countByTicketId(Long ticketId);

        // Запись с деталями
        @Query("SELECT t FROM TimeEntry t LEFT JOIN FETCH t.ticket LEFT JOIN FETCH t.specialist WHERE t.id = :id")
        Optional<TimeEntry> findByIdWithDetails(@Param("id") Long id);

        // =====================================================================
        // REPORT QUERIES
        // =====================================================================

        // Количество уникальных тикетов по специалисту за период
        @Query("SELECT COUNT(DISTINCT t.ticket.id) FROM TimeEntry t WHERE t.specialist.id = :specialistId AND t.entryDate BETWEEN :from AND :to")
        Long countDistinctTicketsBySpecialistAndPeriod(
                        @Param("specialistId") Long specialistId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        // Сумма времени по линии за период
        @Query("SELECT COALESCE(SUM(t.durationSeconds), 0) FROM TimeEntry t WHERE t.ticket.supportLine.id = :lineId AND t.entryDate BETWEEN :from AND :to")
        Long sumDurationByLineAndPeriod(
                        @Param("lineId") Long lineId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);

        // Количество уникальных тикетов по линии за период
        @Query("SELECT COUNT(DISTINCT t.ticket.id) FROM TimeEntry t WHERE t.ticket.supportLine.id = :lineId AND t.entryDate BETWEEN :from AND :to")
        Long countDistinctTicketsByLineAndPeriod(
                        @Param("lineId") Long lineId,
                        @Param("from") Instant from,
                        @Param("to") Instant to);
}
