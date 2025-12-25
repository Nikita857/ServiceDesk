package com.bm.wschat.feature.report.service;

import com.bm.wschat.feature.report.dto.response.*;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.ticket.repository.TicketStatusHistoryRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * Сервис отчётов.
 * Использует автоматический учёт времени через TicketStatusHistory.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

        private static final String ROLE_SPECIALIST = "SPECIALIST";

        private final TicketStatusHistoryRepository statusHistoryRepository;
        private final TicketRepository ticketRepository;
        private final UserRepository userRepository;
        private final SupportLineRepository supportLineRepository;
        private final AssignmentRepository assignmentRepository;

        // =====================================================================
        // TIME REPORTS (теперь на основе TicketStatusHistory)
        // =====================================================================

        /**
         * Отчет по времени сгруппированный по специалистам.
         * Время считается автоматически на основе истории статусов тикетов.
         */
        public List<TimeReportBySpecialistResponse> getTimeReportBySpecialist(LocalDate from, LocalDate to) {
                Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

                List<User> specialists = userRepository.findByRole(ROLE_SPECIALIST);

                return specialists.stream()
                                .map(specialist -> {
                                        Long totalSeconds = statusHistoryRepository.sumDurationBySpecialistAndPeriod(
                                                        specialist.getId(), fromInstant, toInstant);
                                        Long ticketCount = statusHistoryRepository
                                                        .countDistinctTicketsBySpecialistAndPeriod(
                                                                        specialist.getId(), fromInstant, toInstant);

                                        return new TimeReportBySpecialistResponse(
                                                        specialist.getId(),
                                                        specialist.getUsername(),
                                                        specialist.getFio(),
                                                        totalSeconds != null ? totalSeconds : 0L,
                                                        ticketCount != null ? ticketCount : 0L);
                                })
                                .filter(r -> r.totalSeconds() > 0)
                                .sorted(Comparator.comparing(TimeReportBySpecialistResponse::totalSeconds).reversed())
                                .toList();
        }

        /**
         * Отчет по времени сгруппированный по линиям поддержки.
         * Время считается автоматически на основе истории статусов тикетов.
         */
        public List<TimeReportByLineResponse> getTimeReportByLine(LocalDate from, LocalDate to) {
                Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

                List<SupportLine> lines = supportLineRepository.findAllByOrderByDisplayOrderAsc();

                return lines.stream()
                                .map(line -> {
                                        Long totalSeconds = statusHistoryRepository.sumDurationByLineAndPeriod(
                                                        line.getId(), fromInstant, toInstant);
                                        Long ticketCount = statusHistoryRepository.countDistinctTicketsByLineAndPeriod(
                                                        line.getId(), fromInstant, toInstant);

                                        return new TimeReportByLineResponse(
                                                        line.getId(),
                                                        line.getName(),
                                                        line.getDisplayOrder(),
                                                        totalSeconds != null ? totalSeconds : 0L,
                                                        ticketCount != null ? ticketCount : 0L,
                                                        (long) line.getSpecialists().size());
                                })
                                .toList();
        }

        // =====================================================================
        // TICKET STATISTICS
        // =====================================================================

        /**
         * Статистика тикетов по статусам
         */
        public List<TicketStatsByStatusResponse> getTicketStatsByStatus() {
                List<Object[]> results = ticketRepository.countByStatus();
                long total = results.stream()
                                .mapToLong(r -> (Long) r[1])
                                .sum();

                return results.stream()
                                .map(r -> {
                                        TicketStatus status = (TicketStatus) r[0];
                                        Long count = (Long) r[1];
                                        Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
                                        return new TicketStatsByStatusResponse(status, count, percentage);
                                })
                                .sorted(Comparator.comparing(TicketStatsByStatusResponse::count).reversed())
                                .toList();
        }

        /**
         * Статистика тикетов по категориям пользователей
         */
        public List<TicketStatsByCategoryResponse> getTicketStatsByUserCategory() {
                List<Object[]> results = ticketRepository.countByCategoryUser();
                long total = results.stream()
                                .mapToLong(r -> (Long) r[2])
                                .sum();

                return results.stream()
                                .map(r -> {
                                        Long categoryId = (Long) r[0];
                                        String categoryName = (String) r[1];
                                        Long count = (Long) r[2];
                                        Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
                                        return new TicketStatsByCategoryResponse(categoryId, categoryName, "USER",
                                                        count, percentage);
                                })
                                .toList();
        }

        /**
         * Статистика тикетов по категориям поддержки
         */
        public List<TicketStatsByCategoryResponse> getTicketStatsBySupportCategory() {
                List<Object[]> results = ticketRepository.countByCategorySupport();
                long total = results.stream()
                                .mapToLong(r -> (Long) r[2])
                                .sum();

                return results.stream()
                                .map(r -> {
                                        Long categoryId = (Long) r[0];
                                        String categoryName = (String) r[1];
                                        Long count = (Long) r[2];
                                        Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
                                        return new TicketStatsByCategoryResponse(categoryId, categoryName, "SUPPORT",
                                                        count, percentage);
                                })
                                .toList();
        }

        /**
         * Статистика по времени решения тикетов
         */
        public ResolutionTimeResponse getResolutionTimeStats() {
                List<Object[]> results = ticketRepository.getResolutionTimeStats();

                if (results.isEmpty() || results.get(0)[0] == null) {
                        return new ResolutionTimeResponse(0L, null, null, null, null);
                }

                Object[] row = results.get(0);
                return new ResolutionTimeResponse(
                                ((Number) row[0]).longValue(),
                                row[1] != null ? ((Number) row[1]).doubleValue() : null,
                                row[2] != null ? ((Number) row[2]).doubleValue() : null,
                                row[3] != null ? ((Number) row[3]).doubleValue() : null,
                                null);
        }

        // =====================================================================
        // SPECIALIST WORKLOAD
        // =====================================================================

        /**
         * Загрузка специалистов.
         * Время работы считается автоматически на основе истории статусов.
         */
        public List<SpecialistWorkloadResponse> getSpecialistWorkload() {
                List<User> specialists = userRepository.findByRole(ROLE_SPECIALIST);

                LocalDate today = LocalDate.now();
                Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
                Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

                return specialists.stream()
                                .map(specialist -> {
                                        Long activeTickets = ticketRepository
                                                        .countActiveByAssignedTo(specialist.getId());
                                        Long resolvedToday = ticketRepository.countResolvedBySpecialistAndPeriod(
                                                        specialist.getId(), todayStart, todayEnd);
                                        // Время работы теперь автоматическое из истории статусов
                                        Long totalTimeToday = statusHistoryRepository.sumDurationBySpecialistAndPeriod(
                                                        specialist.getId(), todayStart, todayEnd);
                                        Double avgResolutionTime = ticketRepository
                                                        .getAvgResolutionTimeBySpecialist(specialist.getId());

                                        return new SpecialistWorkloadResponse(
                                                        specialist.getId(),
                                                        specialist.getUsername(),
                                                        specialist.getFio(),
                                                        activeTickets != null ? activeTickets : 0L,
                                                        resolvedToday != null ? resolvedToday : 0L,
                                                        totalTimeToday != null ? totalTimeToday : 0L,
                                                        avgResolutionTime);
                                })
                                .sorted(Comparator.comparing(SpecialistWorkloadResponse::activeTickets).reversed())
                                .toList();
        }

        // =====================================================================
        // TICKET HISTORY REPORTS
        // =====================================================================

        /**
         * Получить все тикеты включая soft-deleted.
         */
        public org.springframework.data.domain.Page<TicketReportListResponse> getAllTicketsIncludingDeleted(
                        org.springframework.data.domain.Pageable pageable) {
                return ticketRepository.findAllIncludingDeleted(pageable)
                                .map(this::mapToReportListResponse);
        }

        /**
         * Получить полную историю тикета.
         */
        public TicketHistoryResponse getTicketHistory(Long ticketId) {
                var ticket = ticketRepository.findByIdIncludingDeleted(ticketId)
                                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                                                "Тикет не найден: " + ticketId));

                var statusHistory = statusHistoryRepository.findByTicketIdOrderByEnteredAtAsc(ticketId);

                // Маппинг истории статусов
                List<TicketStatusHistoryResponse> historyResponses = statusHistory.stream()
                                .map(h -> new TicketStatusHistoryResponse(
                                                h.getStatus().name(),
                                                h.getEnteredAt(),
                                                h.getExitedAt(),
                                                h.getDurationSeconds(),
                                                h.getDurationFormatted(),
                                                h.getChangedBy() != null ? h.getChangedBy().getFio() : null,
                                                h.getComment()))
                                .toList();

                // Время первой реакции (NEW -> OPEN)
                Long firstResponseTime = calculateFirstResponseTime(statusHistory);

                // Время без специалиста
                Long totalUnassigned = ticket.getTotalUnassignedSecondsWithCurrent();

                // Общее активное время
                Long totalActive = statusHistoryRepository.getTotalActiveTime(ticketId);

                return new TicketHistoryResponse(
                                ticket.getId(),
                                ticket.getTitle(),
                                ticket.getStatus().name(),
                                ticket.getPriority().name(),
                                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getFio() : null,
                                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFio() : null,
                                ticket.getSupportLine() != null ? ticket.getSupportLine().getName() : null,
                                ticket.getCreatedAt(),
                                ticket.getResolvedAt(),
                                ticket.getClosedAt(),
                                ticket.getDeletedAt(),
                                firstResponseTime,
                                totalUnassigned,
                                totalActive,
                                historyResponses);
        }

        /**
         * Получить историю переназначений тикета.
         */
        public List<ReassignmentHistoryResponse> getReassignmentHistory(Long ticketId) {
                // Проверяем существование тикета
                ticketRepository.findByIdIncludingDeleted(ticketId)
                                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                                                "Тикет не найден: " + ticketId));

                return assignmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
                                .map(a -> new ReassignmentHistoryResponse(
                                                a.getId(),
                                                a.getFromUser() != null ? a.getFromUser().getFio() : null,
                                                a.getToUser() != null ? a.getToUser().getFio() : null,
                                                a.getFromLine() != null ? a.getFromLine().getName() : null,
                                                a.getToLine() != null ? a.getToLine().getName() : null,
                                                a.getMode().name(),
                                                a.getStatus().name(),
                                                a.getNote(),
                                                a.getCreatedAt(),
                                                a.getAcceptedAt(),
                                                a.getRejectedAt(),
                                                a.getRejectedReason()))
                                .toList();
        }

        // === Private helpers ===

        private TicketReportListResponse mapToReportListResponse(
                        com.bm.wschat.feature.ticket.model.Ticket ticket) {
                return new TicketReportListResponse(
                                ticket.getId(),
                                ticket.getTitle(),
                                ticket.getStatus().name(),
                                ticket.getPriority().name(),
                                ticket.getCreatedBy() != null ? ticket.getCreatedBy().getFio() : null,
                                ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFio() : null,
                                ticket.getSupportLine() != null ? ticket.getSupportLine().getName() : null,
                                ticket.getCreatedAt(),
                                ticket.getClosedAt(),
                                ticket.getDeletedAt(),
                                ticket.getDeletedAt() != null);
        }

        private Long calculateFirstResponseTime(
                        List<com.bm.wschat.feature.ticket.model.TicketStatusHistory> history) {
                if (history.isEmpty()) {
                        return null;
                }
                java.time.Instant createdAt = null;
                java.time.Instant firstOpenAt = null;
                for (var h : history) {
                        if (h.getStatus() == TicketStatus.NEW && createdAt == null) {
                                createdAt = h.getEnteredAt();
                        }
                        if (h.getStatus() == TicketStatus.OPEN && firstOpenAt == null) {
                                firstOpenAt = h.getEnteredAt();
                                break;
                        }
                }
                if (createdAt != null && firstOpenAt != null) {
                        return java.time.Duration.between(createdAt, firstOpenAt).getSeconds();
                }
                return null;
        }
}
