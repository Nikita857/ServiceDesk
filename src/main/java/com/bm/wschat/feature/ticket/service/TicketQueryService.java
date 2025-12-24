package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.mapper.TicketMapper;
import com.bm.wschat.feature.ticket.mapper.assignment.AssignmentMapper;
import com.bm.wschat.feature.ticket.model.Assignment;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для чтения тикетов.
 * Отвечает за получение списков, поиск и доступ к отдельным тикетам.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryService {

    private final TicketRepository ticketRepository;
    private final SupportLineRepository supportLineRepository;
    private final AssignmentRepository assignmentRepository;
    private final TicketMapper ticketMapper;
    private final AssignmentMapper assignmentMapper;
    private final TicketAccessChecker accessChecker;

    /**
     * Получить тикет по ID с проверкой доступа.
     */
    @Cacheable(cacheNames = "ticket", key = "#id")
    public TicketResponse getTicketById(Long id, User user) {
        Ticket ticket = findTicketById(id);

        if (!accessChecker.canAccess(ticket, user)) {
            throw new AccessDeniedException("У вас нет доступа к тикету #" + id);
        }

        return toResponseWithAssignment(ticket);
    }

    /**
     * Получить тикет без проверки доступа (для внутреннего использования).
     */
    public Ticket findTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));
    }

    /**
     * Преобразовать тикет в ответ с последним назначением.
     */
    public TicketResponse toResponseWithAssignment(Ticket ticket) {
        TicketResponse response = ticketMapper.toResponse(ticket);

        Assignment lastAssignment = assignmentRepository
                .findLatestByTicketId(ticket.getId())
                .orElse(null);

        if (lastAssignment != null) {
            return new TicketResponse(
                    response.id(),
                    response.title(),
                    response.description(),
                    response.link1c(),
                    response.status(),
                    response.priority(),
                    response.createdBy(),
                    response.assignedTo(),
                    response.supportLine(),
                    response.categoryUser(),
                    response.categorySupport(),
                    response.timeSpentSeconds(),
                    response.messageCount(),
                    response.attachmentCount(),
                    response.slaDeadline(),
                    response.resolvedAt(),
                    response.closedAt(),
                    response.createdAt(),
                    response.updatedAt(),
                    assignmentMapper.toResponse(lastAssignment));
        }

        return response;
    }

    // === Списки тикетов ===

    /**
     * Все тикеты (для админов).
     */
    public Page<TicketListResponse> listTickets(Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findAll(pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    /**
     * Тикеты созданные пользователем.
     */
    public Page<TicketListResponse> getMyTickets(Long userId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByCreatedById(userId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    /**
     * Тикеты назначенные на специалиста.
     */
    public Page<TicketListResponse> getAssignedTickets(Long specialistId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByAssignedToId(specialistId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    /**
     * Тикеты по статусу.
     */
    public Page<TicketListResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByStatus(status, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    /**
     * Тикеты по линии поддержки.
     */
    public Page<TicketListResponse> getTicketsByLine(Long lineId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findBySupportLineId(lineId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    /**
     * Тикеты видимые пользователю:
     * - USER: только свои
     * - SPECIALIST: из своих линий + назначенные
     * - ADMIN: все
     */
    public Page<TicketListResponse> getVisibleTickets(User user, Pageable pageable) {
        if (user.isAdmin()) {
            return listTickets(pageable);
        }

        if (user.isSpecialist()) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            if (userLines.isEmpty()) {
                return getAssignedTickets(user.getId(), pageable);
            }
            Page<Ticket> tickets = ticketRepository.findVisibleToSpecialist(userLines, user.getId(), pageable);
            return tickets.map(ticketMapper::toListResponse);
        }

        return getMyTickets(user.getId(), pageable);
    }
}
