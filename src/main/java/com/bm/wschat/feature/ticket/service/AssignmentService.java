package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentRejectRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.mapper.TicketMapper;
import com.bm.wschat.feature.ticket.mapper.assignment.AssignmentMapper;
import com.bm.wschat.feature.ticket.model.*;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import com.bm.wschat.shared.messaging.TicketEventPublisher;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Сервис управления назначениями тикетов.
 * Тикеты назначаются либо на линию поддержки (специалисты берут сами),
 * либо на конкретного специалиста (с проверкой его доступности).
 */
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
    private final TicketMapper ticketMapper;
    private final UserActivityStatusService userActivityStatusService;
    private final ForwardingRulesService forwardingRulesService;
    private final TicketTimeTrackingService ticketTimeTrackingService;
    private final TicketEventPublisher ticketEventPublisher;

    /**
     * Создать назначение тикета
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#request.ticketId()")
    public AssignmentResponse createAssignment(AssignmentCreateRequest request, Long assignedById) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + request.ticketId()));

        SupportLine toLine = supportLineRepository.findById(request.toLineId())
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + request.toLineId()));

        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + assignedById));

        if (ticket.getStatus().equals(TicketStatus.CLOSED) || ticket.getStatus().equals(TicketStatus.RESOLVED)) {
            throw new IllegalStateException(
                    "Невозможно переназначить тикет со статусом 'Закрыт' или 'Решён'");
        }

        // Проверка нет ли уже ожидающего назначения
        if (assignmentRepository.existsByTicketIdAndStatus(request.ticketId(), AssignmentStatus.PENDING)) {
            throw new IllegalStateException("Тикет уже имеет ожидающее назначение");
        }

        // Получаем линию-источник
        SupportLine fromLine = supportLineRepository.findById(request.fromLineId()).orElseThrow(
                () -> new EntityNotFoundException("Линия поддержки не найдена: " + request.fromLineId()));

        // Проверка правил переадресации на основе ролей
        // SYSADMIN → 1CSUPPORT
        // 1CSUPPORT → SYSADMIN, DEV1C
        // DEV1C → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
        // DEVELOPER → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
        // ADMIN → любая линия
        forwardingRulesService.validateForwarding(assignedBy, fromLine, toLine);

        Assignment.AssignmentBuilder builder = Assignment.builder()
                .ticket(ticket)
                .toLine(toLine)
                .note(request.note())
                .mode(request.mode() != null ? request.mode() : AssignmentMode.FIRST_AVAILABLE)
                .status(AssignmentStatus.PENDING);

        // Откуда назначаем
        builder.fromLine(fromLine);
        builder.fromUser(assignedBy);

        // Если указан конкретный специалист - назначаем напрямую
        if (request.toUserId() != null) {
            User toUser = userRepository.findById(request.toUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + request.toUserId()));

            if (!toUser.isSpecialist()) {
                throw new IllegalArgumentException("Указанный пользователь не является специалистом");
            }

            // Проверка что специалист в этой линии
            if (!toLine.getSpecialists().contains(toUser)) {
                throw new IllegalArgumentException("Специалист не входит в выбранную линию поддержки");
            }

            // Проверка доступности специалиста по статусу активности
            if (!userActivityStatusService.isAvailableForAssignment(toUser.getId())) {
                UserActivityStatus status = userActivityStatusService.getStatus(toUser.getId());
                throw new IllegalStateException(
                        "Специалист " + toUser.getUsername() + " недоступен для назначения тикетов. " +
                                "Текущий статус: " + status);
            }

            builder.toUser(toUser);
            builder.mode(AssignmentMode.DIRECT);
        }
        // Если специалист не указан - тикет назначается на линию
        // Специалисты линии будут брать тикеты самостоятельно

        Assignment saved = assignmentRepository.save(builder.build());

        // Обновить статус тикета
        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);
            // Фиксируем смену статуса
            ticketTimeTrackingService.recordStatusChange(ticket, TicketStatus.OPEN, assignedBy,
                    "Назначение создано: " + toLine.getName());
        }
        ticket.setSupportLine(toLine);
        ticketRepository.save(ticket);

        log.info("Назначение создано: ticket={}, toLine={}, toUser={}",
                ticket.getId(), toLine.getName(),
                saved.getToUser() != null ? saved.getToUser().getUsername() : "auto");

        // RabbitMQ: уведомляем об обновлении тикета
        ticketEventPublisher.publishAssigned(ticket.getId(), assignedById, ticketMapper.toResponse(ticket));

        return assignmentMapper.toResponse(saved);
    }

    /**
     * Принять назначение
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", allEntries = true)
    public AssignmentResponse acceptAssignment(Long assignmentId, Long userId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Назначение не найдено: " + assignmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверка что назначено этому пользователю или на линию где он есть
        if (!canAccept(assignment, user)) {
            throw new AccessDeniedException("Вы не можете принять это назначение");
        }

        if (!assignment.isPending()) {
            throw new IllegalStateException("Назначение не находится в ожидании");
        }

        assignment.accept();
        if (assignment.getToUser() == null) {
            assignment.setToUser(user); // Если было на линию — теперь конкретный
        }
        Assignment saved = assignmentRepository.save(assignment);

        // Обновить тикет
        Ticket ticket = assignment.getTicket();
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setAssignedToWithTracking(user);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);

        // Фиксируем смену статуса если изменился
        if (oldStatus != TicketStatus.OPEN) {
            ticketTimeTrackingService.recordStatusChange(ticket, TicketStatus.OPEN, user,
                    "Назначение принято");
        }

        log.info("Assignment accepted: id={}, by={}", assignmentId, user.getUsername());

        // RabbitMQ: уведомляем об обновлении тикета
        ticketEventPublisher.publishAssigned(ticket.getId(), userId, ticketMapper.toResponse(ticket));

        return assignmentMapper.toResponse(saved);
    }

    /**
     * Отклонить назначение
     */
    @Transactional
    public AssignmentResponse rejectAssignment(Long assignmentId, AssignmentRejectRequest request, Long userId) {
        Assignment assignment = assignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Назначение не найдено: " + assignmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (!canAccept(assignment, user)) {
            throw new AccessDeniedException("Вы не можете отклонить это назначение");
        }

        if (!assignment.isPending()) {
            throw new IllegalStateException("Назначение не находится в ожидании");
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
            ticket.setAssignedToWithTracking(assignment.getFromUser());
        } else {
            // Если конкретного отправителя не было - убираем назначение
            ticket.setAssignedToWithTracking(null);
        }

        // Убедимся что тикет виден (статус не ESCALATED если вернулся)
        if (ticket.getStatus() == TicketStatus.ESCALATED) {
            ticket.setStatus(TicketStatus.OPEN);
            // Фиксируем смену статуса
            ticketTimeTrackingService.recordStatusChange(ticket, TicketStatus.OPEN, user,
                    "Назначение отклонено: " + request.reason());
        }

        ticketRepository.save(ticket);

        log.info("Назначение отклонено: id={}, кем={}, причина={}. Тикет возвращен на линию={}, пользователю={}",
                assignmentId, user.getUsername(), request.reason(),
                assignment.getFromLine() != null ? assignment.getFromLine().getName() : "none",
                assignment.getFromUser() != null ? assignment.getFromUser().getUsername() : "none");

        // RabbitMQ: уведомляем об обновлении тикета
        ticketEventPublisher.publishUpdated(ticket.getId(), userId, ticketMapper.toResponse(ticket));

        return assignmentMapper.toResponse(saved);
    }

    /**
     * История назначений тикета
     */
    public List<AssignmentResponse> getTicketAssignments(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Тикет не найден: " + ticketId);
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
                .orElseThrow(() -> new EntityNotFoundException("Назначение не найдено: " + assignmentId));
        return assignmentMapper.toResponse(assignment);
    }

    /**
     * Текущее назначение тикета
     */
    public AssignmentResponse getCurrentAssignment(Long ticketId) {
        Assignment assignment = assignmentRepository.findCurrentByTicketId(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Нет текущих нащзначений для тикета: " + ticketId));
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
}
