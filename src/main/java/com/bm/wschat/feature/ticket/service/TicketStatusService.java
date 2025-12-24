package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.notification.model.Notification;
import com.bm.wschat.feature.notification.service.NotificationService;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.request.ChangeStatusRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.mapper.TicketMapper;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.messaging.TicketEventPublisher;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Сервис для управления статусами тикетов.
 * 
 * Реализует двухфакторное закрытие:
 * 1. Специалист: RESOLVED → PENDING_CLOSURE
 * 2. Пользователь: PENDING_CLOSURE → CLOSED/REOPENED
 * 3. Админ: принудительное закрытие из любого статуса
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketStatusService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportLineRepository supportLineRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final TicketTimeTrackingService timeTrackingService;
    private final TicketEventPublisher ticketEventPublisher;

    // === Status workflow ===
    private static final Set<TicketStatus> ALLOWED_FROM_NEW = Set.of(
            TicketStatus.OPEN, TicketStatus.REJECTED, TicketStatus.CANCELLED);
    private static final Set<TicketStatus> ALLOWED_FROM_OPEN = Set.of(
            TicketStatus.PENDING, TicketStatus.RESOLVED, TicketStatus.ESCALATED);
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING = Set.of(
            TicketStatus.OPEN);
    private static final Set<TicketStatus> ALLOWED_FROM_ESCALATED = Set.of(
            TicketStatus.OPEN, TicketStatus.PENDING, TicketStatus.RESOLVED);
    private static final Set<TicketStatus> ALLOWED_FROM_RESOLVED = Set.of(
            TicketStatus.PENDING_CLOSURE, TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING_CLOSURE = Set.of(
            TicketStatus.CLOSED, TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_CLOSED = Set.of(
            TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_REOPENED = Set.of(
            TicketStatus.OPEN, TicketStatus.RESOLVED, TicketStatus.PENDING,
            TicketStatus.ESCALATED, TicketStatus.CANCELLED);
    private static final Set<TicketStatus> ALLOWED_FROM_REJECTED = Set.of();
    private static final Set<TicketStatus> ALLOWED_FROM_CANCELLED = Set.of();

    /**
     * Изменить статус тикета с проверкой прав и валидацией перехода.
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse changeStatus(Long id, User user, ChangeStatusRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();

        // Валидация перехода
        validateStatusTransition(ticket, user, currentStatus, newStatus);

        // Обработка специфичной логики статуса
        processStatusChange(ticket, user, currentStatus, newStatus);

        // Записываем смену статуса в историю
        timeTrackingService.recordStatusChange(ticket, newStatus, user, request.comment());

        ticket.setStatus(newStatus);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        // Уведомления
        sendStatusChangeNotification(updated, currentStatus, newStatus);

        TicketResponse response = ticketMapper.toResponse(updated);
        ticketEventPublisher.publishStatusChanged(updated.getId(), user.getId(), response);

        log.info("Статус тикета #{} изменён: {} → {} (user={})",
                id, currentStatus, newStatus, user.getUsername());

        return response;
    }

    /**
     * Взять тикет в работу (стать исполнителем).
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#ticketId")
    public TicketResponse takeTicket(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        validateTakeTicket(ticket, specialist);

        // Назначаем специалиста
        ticket.setAssignedToWithTracking(specialist);

        // Если тикет новый - переводим в OPEN
        TicketStatus oldStatus = ticket.getStatus();
        if (oldStatus == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);

            if (ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseAt(Instant.now());
            }

            timeTrackingService.recordStatusChange(ticket, TicketStatus.OPEN, specialist, "Тикет взят в работу");
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(updated);
        ticketEventPublisher.publishAssigned(updated.getId(), specialist.getId(), response);

        log.info("Тикет #{} взят в работу специалистом {}", ticketId, specialist.getUsername());

        return response;
    }

    // === Validation ===

    private void validateStatusTransition(Ticket ticket, User user,
            TicketStatus currentStatus, TicketStatus newStatus) {
        // Админ может закрыть принудительно из любого статуса
        boolean isAdminForceClose = user.isAdmin() &&
                newStatus == TicketStatus.CLOSED &&
                currentStatus != TicketStatus.PENDING_CLOSURE;

        if (!isAdminForceClose && !isValidTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                    "Некорректный переход статусов: " + currentStatus + " → " + newStatus);
        }

        boolean isCreator = ticket.getCreatedBy() != null &&
                ticket.getCreatedBy().getId().equals(user.getId());
        boolean isAssignee = ticket.getAssignedTo() != null &&
                ticket.getAssignedTo().getId().equals(user.getId());
        boolean isAdmin = user.isAdmin();

        // Проверка прав на смену статуса
        if (newStatus == TicketStatus.PENDING_CLOSURE) {
            if (!isAssignee && !isAdmin) {
                throw new AccessDeniedException("Только исполнитель может запросить закрытие тикета");
            }
        } else if (currentStatus == TicketStatus.PENDING_CLOSURE) {
            if (!isCreator && !isAdmin) {
                throw new AccessDeniedException("Только создатель может подтвердить/отклонить закрытие");
            }
        } else {
            if (!isAssignee && !isAdmin) {
                throw new AccessDeniedException("У вас нет права управлять этим тикетом");
            }
        }
    }

    private void validateTakeTicket(Ticket ticket, User specialist) {
        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("Только специалисты могут брать тикеты в работу");
        }

        if (ticket.getAssignedTo() != null) {
            String assigneeName = ticket.getAssignedTo().getFio() != null
                    ? ticket.getAssignedTo().getFio()
                    : ticket.getAssignedTo().getUsername();
            throw new IllegalStateException("Тикет уже назначен на " + assigneeName);
        }

        if (ticket.getSupportLine() != null) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(specialist);
            boolean inSameLine = userLines.stream()
                    .anyMatch(line -> line.getId().equals(ticket.getSupportLine().getId()));

            if (!inSameLine) {
                throw new AccessDeniedException("Вы не входите в линию поддержки этого тикета");
            }
        }
    }

    private boolean isValidTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case NEW -> ALLOWED_FROM_NEW.contains(to);
            case OPEN -> ALLOWED_FROM_OPEN.contains(to);
            case PENDING -> ALLOWED_FROM_PENDING.contains(to);
            case ESCALATED -> ALLOWED_FROM_ESCALATED.contains(to);
            case RESOLVED -> ALLOWED_FROM_RESOLVED.contains(to);
            case PENDING_CLOSURE -> ALLOWED_FROM_PENDING_CLOSURE.contains(to);
            case CLOSED -> ALLOWED_FROM_CLOSED.contains(to);
            case REOPENED -> ALLOWED_FROM_REOPENED.contains(to);
            case REJECTED -> ALLOWED_FROM_REJECTED.contains(to);
            case CANCELLED -> ALLOWED_FROM_CANCELLED.contains(to);
        };
    }

    // === Status processing ===

    private void processStatusChange(Ticket ticket, User user,
            TicketStatus currentStatus, TicketStatus newStatus) {
        if (newStatus == TicketStatus.PENDING_CLOSURE) {
            ticket.setClosureRequestedBy(user);
            ticket.setClosureRequestedAt(Instant.now());
        } else if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
            ticket.setClosureRequestedBy(null);
            ticket.setClosureRequestedAt(null);
        } else if (newStatus == TicketStatus.REOPENED) {
            ticket.setClosedAt(null);
            ticket.setResolvedAt(null);
            ticket.setClosureRequestedBy(null);
            ticket.setClosureRequestedAt(null);
        }
    }

    // === Notifications ===

    private void sendStatusChangeNotification(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus) {
        Notification notification = Notification.statusChange(
                ticket.getId(),
                ticket.getTitle(),
                oldStatus.name(),
                newStatus.name());

        if (ticket.getCreatedBy() != null) {
            notificationService.notifyUser(ticket.getCreatedBy().getId(), notification);
        }

        if (ticket.getAssignedTo() != null &&
                (ticket.getCreatedBy() == null ||
                        !ticket.getAssignedTo().getId().equals(ticket.getCreatedBy().getId()))) {
            notificationService.notifyUser(ticket.getAssignedTo().getId(), notification);
        }
    }

    // === Cancel ticket ===

    /**
     * Отменить тикет (только создатель может отменить свой тикет).
     * Переводит в статус CANCELLED и делает soft delete.
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#ticketId")
    public TicketResponse cancelTicket(Long ticketId, User user, String reason) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        // Только создатель может отменить
        if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Только создатель тикета может его отменить");
        }

        TicketStatus currentStatus = ticket.getStatus();

        // Нельзя отменить уже закрытый/отменённый тикет
        if (currentStatus.isFinal()) {
            throw new IllegalStateException("Нельзя отменить тикет в статусе " + currentStatus);
        }

        // Записываем смену статуса
        timeTrackingService.recordStatusChange(ticket, TicketStatus.CANCELLED, user, reason);

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setDeletedAt(Instant.now()); // soft delete
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        log.info("Тикет {} отменён пользователем {}. Причина: {}", ticketId, user.getUsername(), reason);

        // Уведомить назначенного специалиста если есть
        if (ticket.getAssignedTo() != null) {
            Notification notification = Notification.statusChange(
                    ticketId, ticket.getTitle(), currentStatus.name(), TicketStatus.CANCELLED.name());
            notificationService.notifyUser(ticket.getAssignedTo().getId(), notification);
        }

        TicketResponse response = ticketMapper.toResponse(updated);
        ticketEventPublisher.publishStatusChanged(ticketId, user.getId(), response);

        return response;
    }
}
