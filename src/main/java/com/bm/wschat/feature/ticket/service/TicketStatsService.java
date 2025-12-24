package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.response.LineTicketStatsResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.UserTicketStatsResponse;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для получения статистики тикетов.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketStatsService {

    private final TicketRepository ticketRepository;
    private final SupportLineRepository supportLineRepository;

    // === Line Statistics ===

    /**
     * Статистика по всем линиям поддержки.
     */
    public List<LineTicketStatsResponse> getStatsForAllLines() {
        List<SupportLine> lines = supportLineRepository.findAllByOrderByDisplayOrderAsc().stream()
                .filter(line -> line.getDeletedAt() == null)
                .toList();
        return lines.stream()
                .map(this::getStatsForLine)
                .toList();
    }

    /**
     * Статистика для конкретной линии.
     */
    public LineTicketStatsResponse getStatsForLine(Long lineId) {
        SupportLine line = supportLineRepository.findById(lineId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Линия не найдена: " + lineId));
        return getStatsForLine(line);
    }

    private LineTicketStatsResponse getStatsForLine(SupportLine line) {
        Long lineId = line.getId();

        List<Object[]> statusCounts = ticketRepository.countByStatusAndLineId(lineId);
        Map<String, Long> byStatus = parseStatusCounts(statusCounts);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long newTickets = byStatus.getOrDefault(TicketStatus.NEW.name(), 0L);
        long open = sumStatuses(byStatus, TicketStatus.OPEN, TicketStatus.PENDING, TicketStatus.ESCALATED);
        long resolved = sumStatuses(byStatus, TicketStatus.RESOLVED, TicketStatus.PENDING_CLOSURE);
        long closed = byStatus.getOrDefault(TicketStatus.CLOSED.name(), 0L);
        Long unassigned = ticketRepository.countUnassignedByLineId(lineId);

        return new LineTicketStatsResponse(
                lineId,
                line.getName(),
                total,
                open,
                resolved,
                closed,
                unassigned != null ? unassigned : 0L,
                newTickets,
                byStatus);
    }

    // === User Statistics ===

    /**
     * Статистика по тикетам текущего пользователя.
     */
    public UserTicketStatsResponse getMyStats(User user) {
        List<Object[]> statusCounts = ticketRepository.countByStatusAndCreatedById(user.getId());
        Map<String, Long> byStatus = parseStatusCounts(statusCounts);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long waiting = byStatus.getOrDefault(TicketStatus.NEW.name(), 0L);
        long open = sumStatuses(byStatus, TicketStatus.OPEN, TicketStatus.PENDING, TicketStatus.ESCALATED);
        long resolved = sumStatuses(byStatus, TicketStatus.RESOLVED, TicketStatus.PENDING_CLOSURE);
        long closed = byStatus.getOrDefault(TicketStatus.CLOSED.name(), 0L);

        return new UserTicketStatsResponse(
                user.getId(),
                user.getUsername(),
                total,
                open,
                resolved,
                closed,
                waiting,
                byStatus);
    }

    /**
     * Общая статистика для дашборда (все тикеты).
     */
    public UserTicketStatsResponse getGlobalStats() {
        List<Object[]> statusCounts = ticketRepository.countByStatus();
        Map<String, Long> byStatus = parseStatusCounts(statusCounts);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long waiting = byStatus.getOrDefault(TicketStatus.NEW.name(), 0L);
        long open = sumStatuses(byStatus, TicketStatus.OPEN, TicketStatus.PENDING, TicketStatus.ESCALATED);
        long resolved = sumStatuses(byStatus, TicketStatus.RESOLVED, TicketStatus.PENDING_CLOSURE);
        long closed = byStatus.getOrDefault(TicketStatus.CLOSED.name(), 0L);

        return new UserTicketStatsResponse(
                null,
                "all",
                total,
                open,
                resolved,
                closed,
                waiting,
                byStatus);
    }

    // === Helpers ===

    private Map<String, Long> parseStatusCounts(List<Object[]> statusCounts) {
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : statusCounts) {
            TicketStatus status = (TicketStatus) row[0];
            Long count = (Long) row[1];
            byStatus.put(status.name(), count);
        }
        return byStatus;
    }

    private long sumStatuses(Map<String, Long> byStatus, TicketStatus... statuses) {
        long sum = 0;
        for (TicketStatus status : statuses) {
            sum += byStatus.getOrDefault(status.name(), 0L);
        }
        return sum;
    }
}
