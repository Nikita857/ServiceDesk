package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentRejectRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.mapper.assignment.AssignmentMapper;
import com.bm.wschat.feature.ticket.model.Assignment;
import com.bm.wschat.feature.ticket.model.AssignmentMode;
import com.bm.wschat.feature.ticket.model.AssignmentStatus;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
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

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final TicketRepository ticketRepository;
    private final SupportLineRepository supportLineRepository;
    private final UserRepository userRepository;
    private final AssignmentMapper assignmentMapper;

    /**
     * Создать назначение тикета
     */
    @Transactional
    public AssignmentResponse createAssignment(AssignmentCreateRequest request, Long assignedById) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + request.ticketId()));

        SupportLine toLine = supportLineRepository.findById(request.toLineId())
                .orElseThrow(() -> new EntityNotFoundException("Support line not found: " + request.toLineId()));

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + assignedById));

        // Проверка нет ли уже ожидающего назначения
        if (assignmentRepository.existsByTicketIdAndStatus(request.ticketId(), AssignmentStatus.PENDING)) {
            throw new IllegalStateException("Ticket already has pending assignment");
        }

        // Получаем линию-источник
        SupportLine fromLine = supportLineRepository.findById(request.fromLineId()).orElseThrow(
                () -> new EntityNotFoundException("From line not found: " + request.fromLineId()));

        // Получаем displayOrder с дефолтными значениями (null = 0)
        int fromOrder = fromLine.getDisplayOrder() != null ? fromLine.getDisplayOrder() : 0;
        int toOrder = toLine.getDisplayOrder() != null ? toLine.getDisplayOrder() : 0;

        // Проверка: нельзя переадресовать на линию с более низким уровнем (меньшим
        // displayOrder)
        // 1 линия (SYSADMIN) = displayOrder 1 (низкий уровень, сюда идут все тикеты по
        // умолчанию)
        // 3 линия (DEVELOPER) = displayOrder 3 (высокий уровень, эксперты)
        // Переадресация возможна только ВВЕРХ (на более высокий displayOrder)
        if (toOrder < fromOrder) {
            throw new IllegalArgumentException(
                    "Cannot reassign ticket to a lower support line. " +
                            "Current line level: " + fromOrder +
                            ", Target line level: " + toOrder);
        }

        // Если toOrder == fromOrder - это переназначение на ту же линию (разрешено,
        // например другому специалисту)

        Assignment.AssignmentBuilder builder = Assignment.builder()
                .ticket(ticket)
                .toLine(toLine)
                .note(request.note())
                .mode(request.mode() != null ? request.mode() : AssignmentMode.FIRST_AVAILABLE)
                .status(AssignmentStatus.PENDING);

        // Откуда назначаем
        builder.fromLine(fromLine);
        builder.fromUser(assignedBy);

        // Если указан конкретный специалист
        if (request.toUserId() != null) {
            User toUser = userRepository.findById(request.toUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.toUserId()));

            if (!toUser.isSpecialist()) {
                throw new IllegalArgumentException("Target user is not a specialist");
            }

            // Проверка что специалист в этой линии
            if (!toLine.getSpecialists().contains(toUser)) {
                throw new IllegalArgumentException("Specialist is not in the target support line");
            }

            builder.toUser(toUser);
            builder.mode(AssignmentMode.DIRECT);
        } else if (request.mode() == AssignmentMode.FIRST_AVAILABLE || request.mode() == null) {
            // Авто-выбор специалиста
            User autoAssigned = findBestSpecialist(toLine);
            if (autoAssigned != null) {
                builder.toUser(autoAssigned);
            }
        }

        Assignment saved = assignmentRepository.save(builder.build());

        // Обновить статус тикета
        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        ticket.setSupportLine(toLine);
        ticketRepository.save(ticket);

        log.info("Assignment created: ticket={}, toLine={}, toUser={}",
                ticket.getId(), toLine.getName(),
                saved.getToUser() != null ? saved.getToUser().getUsername() : "auto");

        return assignmentMapper.toResponse(saved);
    }

    /**
     * Принять назначение
     */
    @Transactional
    public AssignmentResponse acceptAssignment(Long assignmentId, Long userId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Проверка что назначено этому пользователю или на линию где он есть
        if (!canAccept(assignment, user)) {
            throw new AccessDeniedException("You cannot accept this assignment");
        }

        if (!assignment.isPending()) {
            throw new IllegalStateException("Assignment is not pending");
        }

        assignment.accept();
        if (assignment.getToUser() == null) {
            assignment.setToUser(user); // Если было на линию — теперь конкретный
        }
        Assignment saved = assignmentRepository.save(assignment);

        // Обновить тикет
        Ticket ticket = assignment.getTicket();
        ticket.setAssignedTo(user);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);

        log.info("Assignment accepted: id={}, by={}", assignmentId, user.getUsername());

        return assignmentMapper.toResponse(saved);
    }

    /**
     * Отклонить назначение
     */
    @Transactional
    public AssignmentResponse rejectAssignment(Long assignmentId, AssignmentRejectRequest request, Long userId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (!canAccept(assignment, user)) {
            throw new AccessDeniedException("You cannot reject this assignment");
        }

        if (!assignment.isPending()) {
            throw new IllegalStateException("Assignment is not pending");
        }

        assignment.reject(request.reason());
        Assignment saved = assignmentRepository.save(assignment);

        // Возвращаем тикет на исходную линию и исходному пользователю
        Ticket ticket = assignment.getTicket();

        // Возвращаем на линию отправителя
        if (assignment.getFromLine() != null) {
            ticket.setSupportLine(assignment.getFromLine());
        }

        // Возвращаем исходному пользователю (если был)
        if (assignment.getFromUser() != null) {
            ticket.setAssignedTo(assignment.getFromUser());
        } else {
            // Если конкретного отправителя не было - убираем назначение
            ticket.setAssignedTo(null);
        }

        // Убедимся что тикет виден (статус не ESCALATED если вернулся)
        if (ticket.getStatus() == TicketStatus.ESCALATED) {
            ticket.setStatus(TicketStatus.OPEN);
        }

        ticketRepository.save(ticket);

        log.info("Assignment rejected: id={}, by={}, reason={}. Ticket returned to line={}, user={}",
                assignmentId, user.getUsername(), request.reason(),
                assignment.getFromLine() != null ? assignment.getFromLine().getName() : "none",
                assignment.getFromUser() != null ? assignment.getFromUser().getUsername() : "none");

        return assignmentMapper.toResponse(saved);
    }

    /**
     * История назначений тикета
     */
    public List<AssignmentResponse> getTicketAssignments(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Ticket not found: " + ticketId);
        }
        List<Assignment> assignments = assignmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        return assignmentMapper.toResponses(assignments);
    }

    /**
     * Мои ожидающие назначения
     */
    public Page<AssignmentResponse> getMyPendingAssignments(Long userId, Pageable pageable) {
        Page<Assignment> assignments = assignmentRepository
                .findByToUserIdAndStatusOrderByCreatedAtDesc(userId, AssignmentStatus.PENDING, pageable);
        return assignments.map(assignmentMapper::toResponse);
    }

    /**
     * Получить назначение по ID
     */
    public AssignmentResponse getById(Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assignment not found: " + assignmentId));
        return assignmentMapper.toResponse(assignment);
    }

    /**
     * Текущее назначение тикета
     */
    public AssignmentResponse getCurrentAssignment(Long ticketId) {
        Assignment assignment = assignmentRepository.findCurrentByTicketId(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("No current assignment for ticket: " + ticketId));
        return assignmentMapper.toResponse(assignment);
    }

    /**
     * Количество ожидающих назначений
     */
    public Long getPendingCount(Long userId) {
        return assignmentRepository.countByToUserIdAndStatus(userId, AssignmentStatus.PENDING);
    }

    // === Private helpers ===

    private boolean canAccept(Assignment assignment, User user) {
        // Назначено напрямую этому пользователю
        if (assignment.getToUser() != null && assignment.getToUser().getId().equals(user.getId())) {
            return true;
        }

        // Назначено на линию, где пользователь специалист
        if (assignment.getToUser() == null && assignment.getToLine() != null) {
            Set<User> specialists = assignment.getToLine().getSpecialists();
            return specialists != null && specialists.contains(user);
        }

        // Админ может всё
        return user.isAdmin();
    }

    private User findBestSpecialist(SupportLine line) {
        Set<User> specialists = line.getSpecialists();
        if (specialists == null || specialists.isEmpty()) {
            return null;
        }

        // Простая логика: выбрать с минимальной нагрузкой
        User best = null;
        long minLoad = Long.MAX_VALUE;

        for (User specialist : specialists) {
            Long activeCount = assignmentRepository.countActiveByUserId(specialist.getId());
            if (activeCount < minLoad) {
                minLoad = activeCount;
                best = specialist;
            }
        }

        return best;
    }
}
