package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.model.TicketStatusHistory;
import com.bm.wschat.feature.ticket.repository.TicketStatusHistoryRepository;
import com.bm.wschat.feature.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для учёта времени тикета в каждом статусе.
 * Автоматически записывает историю при смене статуса,
 * вычисляет время в каждом статусе и общее активное время.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketTimeTrackingService {

    private final TicketStatusHistoryRepository historyRepository;

    /**
     * Записать начальный статус при создании тикета.
     */
    public void recordInitialStatus(Ticket ticket, User createdBy) {
        TicketStatusHistory history = TicketStatusHistory.builder()
                .ticket(ticket)
                .status(ticket.getStatus())
                .enteredAt(ticket.getCreatedAt() != null ? ticket.getCreatedAt() : Instant.now())
                .changedBy(createdBy)
                .comment("Тикет создан")
                .build();

        historyRepository.save(history);
        log.debug("Recorded initial status {} for ticket {}", ticket.getStatus(), ticket.getId());
    }

    /**
     * Записать смену статуса.
     * Закрывает предыдущую запись и создаёт новую.
     *
     * @param ticket    тикет
     * @param newStatus новый статус
     * @param changedBy кто сменил статус
     * @param comment   комментарий (опционально)
     */
    public void recordStatusChange(Ticket ticket, TicketStatus newStatus, User changedBy, String comment) {
        // Закрываем текущую запись
        closeCurrentStatus(ticket.getId());

        // Создаём новую запись
        TicketStatusHistory history = TicketStatusHistory.builder()
                .ticket(ticket)
                .status(newStatus)
                .enteredAt(Instant.now())
                .changedBy(changedBy)
                .comment(comment)
                .build();

        historyRepository.save(history);
        log.debug("Recorded status change to {} for ticket {} by user {}",
                newStatus, ticket.getId(), changedBy != null ? changedBy.getId() : "system");
    }

    /**
     * Закрыть текущую запись статуса.
     */
    private void closeCurrentStatus(Long ticketId) {
        Optional<TicketStatusHistory> current = historyRepository.findByTicketIdAndExitedAtIsNull(ticketId);
        current.ifPresent(history -> {
            history.close();
            historyRepository.save(history);
            log.debug("Closed status {} for ticket {}, duration: {}s",
                    history.getStatus(), ticketId, history.getDurationSeconds());
        });
    }

    /**
     * Получить историю статусов тикета.
     */
    @Transactional(readOnly = true)
    public List<TicketStatusHistory> getStatusHistory(Long ticketId) {
        return historyRepository.findByTicketIdOrderByEnteredAtAsc(ticketId);
    }

    /**
     * Получить суммарное время в конкретном статусе.
     */
    @Transactional(readOnly = true)
    public long getTotalTimeInStatus(Long ticketId, TicketStatus status) {
        return historyRepository.getTotalDurationInStatus(ticketId, status);
    }

    /**
     * Получить общее активное время (OPEN + PENDING + ESCALATED).
     */
    @Transactional(readOnly = true)
    public long getTotalActiveTime(Long ticketId) {
        return historyRepository.getTotalActiveTime(ticketId);
    }

    /**
     * Получить время первой реакции (от NEW до первого OPEN).
     */
    @Transactional(readOnly = true)
    public Long getFirstResponseTime(Long ticketId) {
        List<TicketStatusHistory> history = historyRepository.findByTicketIdOrderByEnteredAtAsc(ticketId);

        if (history.isEmpty()) {
            return null;
        }

        // Ищем первую запись NEW и первую запись OPEN
        Instant createdAt = null;
        Instant firstOpenAt = null;

        for (TicketStatusHistory h : history) {
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
