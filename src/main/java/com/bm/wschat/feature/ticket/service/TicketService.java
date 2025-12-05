package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.request.ChangeStatusRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.UpdateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.mapper.TicketMapper;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportLineRepository supportLineRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;

    private static final Set<TicketStatus> ALLOWED_FROM_NEW = Set.of(TicketStatus.IN_PROGRESS);
    private static final Set<TicketStatus> ALLOWED_FROM_IN_PROGRESS = Set.of(TicketStatus.PENDING,
            TicketStatus.RESOLVED);
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING = Set.of(TicketStatus.IN_PROGRESS);
    private static final Set<TicketStatus> ALLOWED_FROM_RESOLVED = Set.of(TicketStatus.CLOSED, TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_CLOSED = Set.of(TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_REOPENED = Set.of(TicketStatus.IN_PROGRESS);

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCreatedBy(creator);

        if (request.supportLineId() != null) {
            SupportLine line = supportLineRepository.findById(request.supportLineId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Support line not found with id: " + request.supportLineId()));
            ticket.setSupportLine(line);

            if (line.getSlaMinutes() != null) {
                ticket.setSlaDeadline(Instant.now().plusSeconds(line.getSlaMinutes() * 60L));
            }
        }

        if (request.categoryUserId() != null) {
            Category category = categoryRepository.findById(request.categoryUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Category not found with id: " + request.categoryUserId()));
            ticket.setCategoryUser(category);
        }

        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.link1c() != null) {
            ticket.setLink1c(request.link1c());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);
    }

    @Transactional
    public TicketResponse changeStatus(Long id, ChangeStatusRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();

        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    @Transactional
    public TicketResponse assignToLine(Long id, Long lineId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        SupportLine line = supportLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + lineId));

        ticket.setSupportLine(line);
        ticket.setAssignedTo(null); // сбрасываем конкретного специалиста

        if (line.getSlaMinutes() != null && ticket.getSlaDeadline() == null) {
            ticket.setSlaDeadline(Instant.now().plusSeconds(line.getSlaMinutes() * 60L));
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    @Transactional
    public TicketResponse assignToSpecialist(Long id, Long specialistId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + specialistId));

        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("User is not a specialist");
        }

        ticket.setAssignedTo(specialist);

        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    @Transactional
    public TicketResponse setUserCategory(Long id, Long categoryId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        ticket.setCategoryUser(category);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    @Transactional
    public TicketResponse setSupportCategory(Long id, Long categoryId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        ticket.setCategorySupport(category);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);
        return ticketMapper.toResponse(updated);
    }

    public Page<TicketListResponse> listTickets(Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findAll(pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    public Page<TicketListResponse> getMyTickets(Long userId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByCreatedById(userId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    public Page<TicketListResponse> getAssignedTickets(Long specialistId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByAssignedToId(specialistId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    public Page<TicketListResponse> getTicketsByStatus(TicketStatus status, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findByStatus(status, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    public Page<TicketListResponse> getTicketsByLine(Long lineId, Pageable pageable) {
        Page<Ticket> tickets = ticketRepository.findBySupportLineId(lineId, pageable);
        return tickets.map(ticketMapper::toListResponse);
    }

    private boolean isValidStatusTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case NEW -> ALLOWED_FROM_NEW.contains(to);
            case IN_PROGRESS -> ALLOWED_FROM_IN_PROGRESS.contains(to);
            case PENDING -> ALLOWED_FROM_PENDING.contains(to);
            case RESOLVED -> ALLOWED_FROM_RESOLVED.contains(to);
            case CLOSED -> ALLOWED_FROM_CLOSED.contains(to);
            case REOPENED -> ALLOWED_FROM_REOPENED.contains(to);
        };
    }
}
