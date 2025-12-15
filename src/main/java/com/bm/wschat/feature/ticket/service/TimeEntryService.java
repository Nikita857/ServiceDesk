package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.ticket.dto.timeentry.request.CreateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.request.UpdateTimeEntryRequest;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeEntryResponse;
import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeTotalResponse;
import com.bm.wschat.feature.ticket.mapper.TimeEntryMapper;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TimeEntry;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.ticket.repository.TimeEntryRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TimeEntryMapper timeEntryMapper;

    /**
     * Добавить запись времени
     */
    @Transactional
    public TimeEntryResponse createTimeEntry(Long ticketId, CreateTimeEntryRequest request, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        TimeEntry timeEntry = TimeEntry.builder()
                .ticket(ticket)
                .specialist(specialist)
                .durationSeconds(request.durationSeconds())
                .note(request.note())
                .entryDate(Instant.now())
                .workDate(request.workDate() != null ? request.workDate() : LocalDate.now())
                .activityType(request.activityType())
                .build();

        TimeEntry saved = timeEntryRepository.save(timeEntry);

        // Обновить total на тикете
        updateTicketTotalTime(ticket);

        log.info("Запись времени создана: ticket={}, user={}, duration={}s",
                ticketId, specialist.getUsername(), request.durationSeconds());

        return timeEntryMapper.toResponse(saved);
    }

    /**
     * Получить запись по ID
     */
    public TimeEntryResponse getById(Long id) {
        TimeEntry entry = timeEntryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись времени не найдена: " + id));
        return timeEntryMapper.toResponse(entry);
    }

    /**
     * Записи тикета
     */
    public List<TimeEntryResponse> getByTicketId(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Тикет не найден: " + ticketId);
        }
        List<TimeEntry> entries = timeEntryRepository.findByTicketIdOrderByEntryDateDesc(ticketId);
        return timeEntryMapper.toResponses(entries);
    }

    /**
     * Записи тикета (с пагинацией)
     */
    public Page<TimeEntryResponse> getByTicketId(Long ticketId, Pageable pageable) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Тикет не найден: " + ticketId);
        }
        Page<TimeEntry> entries = timeEntryRepository.findByTicketIdOrderByEntryDateDesc(ticketId, pageable);
        return entries.map(timeEntryMapper::toResponse);
    }

    /**
     * Мои записи
     */
    public Page<TimeEntryResponse> getMyEntries(Long userId, Pageable pageable) {
        Page<TimeEntry> entries = timeEntryRepository.findBySpecialistIdOrderByEntryDateDesc(userId, pageable);
        return entries.map(timeEntryMapper::toResponse);
    }

    /**
     * Итого по тикету
     */
    public TimeTotalResponse getTimeTotal(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Тикет не найден: " + ticketId);
        }
        Long totalSeconds = timeEntryRepository.sumDurationByTicketId(ticketId);
        Long entryCount = timeEntryRepository.countByTicketId(ticketId);

        return TimeTotalResponse.of(ticketId, totalSeconds, entryCount.intValue());
    }

    /**
     * Обновить запись
     */
    @Transactional
    public TimeEntryResponse updateTimeEntry(Long id, UpdateTimeEntryRequest request, Long userId) {
        TimeEntry entry = timeEntryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись времени не найдена: " + id));

        // Проверка прав
        if (!canModify(entry, userId)) {
            throw new AccessDeniedException("Вы можете редактировать только свои записи времени");
        }

        if (request.durationSeconds() != null) {
            entry.setDurationSeconds(request.durationSeconds());
        }
        if (request.note() != null) {
            entry.setNote(request.note());
        }
        if (request.workDate() != null) {
            entry.setWorkDate(request.workDate());
        }
        if (request.activityType() != null) {
            entry.setActivityType(request.activityType());
        }

        TimeEntry updated = timeEntryRepository.save(entry);

        // Обновить total на тикете
        updateTicketTotalTime(entry.getTicket());

        log.info("Запись времени обновлена: id={}", id);

        return timeEntryMapper.toResponse(updated);
    }

    /**
     * Удалить запись
     */
    @Transactional
    public void deleteTimeEntry(Long id, Long userId) {
        TimeEntry entry = timeEntryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Запись времени не найдена: " + id));

        // Проверка прав
        if (!canModify(entry, userId)) {
            throw new AccessDeniedException("Вы можете удалять только свои записи врмени");
        }

        Ticket ticket = entry.getTicket();
        timeEntryRepository.delete(entry); // Soft delete

        // Обновить total на тикете
        updateTicketTotalTime(ticket);

        log.info("Запись времени удалена: id={}", id);
    }

    // === Private helpers ===

    private boolean canModify(TimeEntry entry, Long userId) {
        // Свой или админ
        if (entry.getSpecialist().getId().equals(userId)) {
            return true;
        }
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.isAdmin();
    }

    private void updateTicketTotalTime(Ticket ticket) {
        Long total = timeEntryRepository.sumDurationByTicketId(ticket.getId());
        ticket.setTimeSpentSeconds(total != null ? total : 0L);
        ticketRepository.save(ticket);
    }
}
