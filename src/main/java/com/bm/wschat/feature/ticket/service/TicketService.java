package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.ticket.dto.ticket.request.ChangeStatusRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.UpdateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Фасад для работы с тикетами.
 * 
 * Делегирует операции в специализированные сервисы:
 * - TicketCrudService — CRUD операции
 * - TicketStatusService — управление статусами
 * - TicketQueryService — запросы и списки
 * - TicketAccessChecker — проверка прав доступа
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketCrudService crudService;
    private final TicketStatusService statusService;
    private final TicketQueryService queryService;
    private final TicketAccessChecker accessChecker;

    // === CRUD ===

    public TicketResponse createTicket(CreateTicketRequest request, Long userId) {
        return crudService.createTicket(request, userId);
    }

    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        return crudService.updateTicket(id, request);
    }

    public void deleteTicket(Long id) {
        crudService.deleteTicket(id);
    }

    public TicketResponse rateTicket(Long ticketId, User user, Integer rating, String feedback) {
        return crudService.rateTicket(ticketId, user, rating, feedback);
    }

    // === Status ===

    public TicketResponse changeStatus(Long id, User user, ChangeStatusRequest request) {
        return statusService.changeStatus(id, user, request);
    }

    public TicketResponse takeTicket(Long ticketId, Long userId) {
        return statusService.takeTicket(ticketId, userId);
    }

    public TicketResponse cancelTicket(Long ticketId, User user, String reason) {
        return statusService.cancelTicket(ticketId, user, reason);
    }

    // === Assignment ===

    public TicketResponse assignToLine(Long id, Long lineId) {
        return crudService.assignToLine(id, lineId);
    }

    public TicketResponse assignToSpecialist(Long id, Long specialistId) {
        return crudService.assignToSpecialist(id, specialistId);
    }

    public TicketResponse setUserCategory(Long id, Long categoryId) {
        return crudService.setUserCategory(id, categoryId);
    }

    public TicketResponse setSupportCategory(Long id, Long categoryId) {
        return crudService.setSupportCategory(id, categoryId);
    }

    // === Query ===

    public TicketResponse getTicketById(Long id, User user) {
        return queryService.getTicketById(id, user);
    }

    public Ticket findTicketById(Long id) {
        return queryService.findTicketById(id);
    }

    public Page<TicketListResponse> listTickets(Pageable pageable) {
        return queryService.listTickets(pageable);
    }

    public Page<TicketListResponse> getMyTickets(Long userId, Pageable pageable) {
        return queryService.getMyTickets(userId, pageable);
    }

    public Page<TicketListResponse> getAssignedTickets(Long specialistId, Pageable pageable) {
        return queryService.getAssignedTickets(specialistId, pageable);
    }

    public Page<TicketListResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        return queryService.getTicketsByStatus(status, pageable);
    }

    public Page<TicketListResponse> getTicketsByLine(Long lineId, Pageable pageable) {
        return queryService.getTicketsByLine(lineId, pageable);
    }

    public Page<TicketListResponse> getVisibleTickets(User user, Pageable pageable) {
        return queryService.getVisibleTickets(user, pageable);
    }

    // === Access ===

    public boolean canAccessTicket(Ticket ticket, User user) {
        return accessChecker.canAccess(ticket, user);
    }

    public TicketResponse toResponseWithAssignment(Ticket ticket) {
        return queryService.toResponseWithAssignment(ticket);
    }
}
