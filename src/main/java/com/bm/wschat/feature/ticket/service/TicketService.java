package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.notification.model.Notification;
import com.bm.wschat.feature.notification.service.NotificationService;
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
import java.util.List;
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
    private final NotificationService notificationService;

    // Status workflow based on TicketStatus enum
    private static final Set<TicketStatus> ALLOWED_FROM_NEW = Set.of(TicketStatus.OPEN, TicketStatus.REJECTED,
            TicketStatus.CANCELLED);
    private static final Set<TicketStatus> ALLOWED_FROM_OPEN = Set.of(TicketStatus.PENDING, TicketStatus.RESOLVED,
            TicketStatus.ESCALATED);
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING = Set.of(TicketStatus.OPEN);
    private static final Set<TicketStatus> ALLOWED_FROM_ESCALATED = Set.of(TicketStatus.OPEN, TicketStatus.PENDING,
            TicketStatus.RESOLVED);
    private static final Set<TicketStatus> ALLOWED_FROM_RESOLVED = Set.of(TicketStatus.CLOSED, TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_CLOSED = Set.of(TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_REOPENED = Set.of(TicketStatus.OPEN);
    private static final Set<TicketStatus> ALLOWED_FROM_REJECTED = Set.of();
    private static final Set<TicketStatus> ALLOWED_FROM_CANCELLED = Set.of();

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

        // Прямое назначение специалисту при создании
        if (request.assignToUserId() != null) {
            User specialist = userRepository.findById(request.assignToUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Specialist not found with id: " + request.assignToUserId()));

            if (!specialist.isSpecialist()) {
                throw new IllegalArgumentException("User is not a specialist");
            }

            // Проверка что специалист принадлежит выбранной линии (если линия указана)
            if (ticket.getSupportLine() != null &&
                    !ticket.getSupportLine().getSpecialists().contains(specialist)) {
                throw new IllegalArgumentException("Specialist is not in the selected support line");
            }

            ticket.setAssignedTo(specialist);
            ticket.setStatus(TicketStatus.OPEN);
        }

        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));
        return ticketMapper.toResponse(ticket);
    }

    /**
     * Get ticket by ID with access check
     */
    public TicketResponse getTicketById(Long id, User user) {
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        if (!canAccessTicket(ticket, user)) {
            throw new org.springframework.security.access.AccessDeniedException("You don't have access to this ticket");
        }

        return ticketMapper.toResponse(ticket);
    }

    /**
     * Check if user can access a specific ticket:
     * - DEVELOPER: can access all
     * - SPECIALIST: can access tickets from their lines or assigned to them
     * - USER: can access only their own tickets
     */
    public boolean canAccessTicket(Ticket ticket, User user) {
        // Admin can access all
        if (user.isAdmin()) {
            return true;
        }

        // Creator can always access their own ticket
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Assigned specialist can access
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) {
            return true;
        }

        // Specialist in the same support line can access (if not assigned to specific
        // person)
        if (user.isSpecialist() && ticket.getSupportLine() != null) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            boolean inSameLine = userLines.stream()
                    .anyMatch(line -> line.getId().equals(ticket.getSupportLine().getId()));

            // Can access if in same line AND ticket is not assigned to someone else
            if (inSameLine && ticket.getAssignedTo() == null) {
                return true;
            }
        }

        return false;
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

        // Уведомление об изменении статуса
        sendStatusChangeNotification(updated, currentStatus, newStatus);

        return ticketMapper.toResponse(updated);
    }

    /**
     * Отправить уведомления об изменении статуса
     */
    private void sendStatusChangeNotification(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus) {
        Notification notification = Notification.statusChange(
                ticket.getId(),
                ticket.getTitle(),
                oldStatus.name(),
                newStatus.name());

        // Уведомляем создателя
        if (ticket.getCreatedBy() != null) {
            notificationService.notifyUser(ticket.getCreatedBy().getId(), notification);
        }

        // Уведомляем назначенного (если отличается от создателя)
        if (ticket.getAssignedTo() != null &&
                (ticket.getCreatedBy() == null
                        || !ticket.getAssignedTo().getId().equals(ticket.getCreatedBy().getId()))) {
            notificationService.notifyUser(ticket.getAssignedTo().getId(), notification);
        }
    }

    @Transactional
    public TicketResponse assignToLine(Long id, Long lineId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + id));

        SupportLine line = supportLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Support line not found with id: " + lineId));

        ticket.setSupportLine(line);
        ticket.setAssignedTo(null);
        ticket.setStatus(TicketStatus.ESCALATED);

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
            ticket.setStatus(TicketStatus.OPEN);
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

    /**
     * Get tickets visible to a user based on their role:
     * - USER: only their own tickets (createdBy)
     * - SYSADMIN/DEV1C: tickets from their support lines + assigned to them
     * - DEVELOPER: all tickets (admin access)
     */
    public Page<TicketListResponse> getVisibleTickets(User user, Pageable pageable) {
        // DEVELOPER (admin) sees all tickets
        if (user.isAdmin()) {
            return listTickets(pageable);
        }

        // Specialist sees tickets from their lines + assigned to them
        if (user.isSpecialist()) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            if (userLines.isEmpty()) {
                // Specialist not in any line - see only assigned
                return getAssignedTickets(user.getId(), pageable);
            }
            Page<Ticket> tickets = ticketRepository.findVisibleToSpecialist(userLines, user.getId(), pageable);
            return tickets.map(ticketMapper::toListResponse);
        }

        // Regular USER sees only their own tickets
        return getMyTickets(user.getId(), pageable);
    }

    private boolean isValidStatusTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case NEW -> ALLOWED_FROM_NEW.contains(to);
            case OPEN -> ALLOWED_FROM_OPEN.contains(to);
            case PENDING -> ALLOWED_FROM_PENDING.contains(to);
            case ESCALATED -> ALLOWED_FROM_ESCALATED.contains(to);
            case RESOLVED -> ALLOWED_FROM_RESOLVED.contains(to);
            case CLOSED -> ALLOWED_FROM_CLOSED.contains(to);
            case REOPENED -> ALLOWED_FROM_REOPENED.contains(to);
            case REJECTED -> ALLOWED_FROM_REJECTED.contains(to);
            case CANCELLED -> ALLOWED_FROM_CANCELLED.contains(to);
        };
    }
}
