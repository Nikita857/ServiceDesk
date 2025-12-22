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
import com.bm.wschat.feature.ticket.mapper.assignment.AssignmentMapper;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
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
    private final AssignmentRepository assignmentRepository;
    private final AssignmentMapper assignmentMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final TicketTimeTrackingService timeTrackingService;
    private final com.bm.wschat.shared.messaging.TicketEventPublisher ticketEventPublisher;

    // Status workflow based on TicketStatus enum
    // Обновлено для двухфакторного закрытия: RESOLVED -> PENDING_CLOSURE -> CLOSED
    private static final Set<TicketStatus> ALLOWED_FROM_NEW = Set.of(TicketStatus.OPEN, TicketStatus.REJECTED,
            TicketStatus.CANCELLED);
    private static final Set<TicketStatus> ALLOWED_FROM_OPEN = Set.of(TicketStatus.PENDING, TicketStatus.RESOLVED,
            TicketStatus.ESCALATED);
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING = Set.of(TicketStatus.OPEN);
    private static final Set<TicketStatus> ALLOWED_FROM_ESCALATED = Set.of(TicketStatus.OPEN, TicketStatus.PENDING,
            TicketStatus.RESOLVED);
    // RESOLVED теперь переходит только в PENDING_CLOSURE (для пользователя) или
    // REOPENED
    private static final Set<TicketStatus> ALLOWED_FROM_RESOLVED = Set.of(TicketStatus.PENDING_CLOSURE,
            TicketStatus.REOPENED);
    // PENDING_CLOSURE -> CLOSED (подтверждено) или REOPENED (отклонено)
    private static final Set<TicketStatus> ALLOWED_FROM_PENDING_CLOSURE = Set.of(TicketStatus.CLOSED,
            TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_CLOSED = Set.of(TicketStatus.REOPENED);
    private static final Set<TicketStatus> ALLOWED_FROM_REOPENED = Set.of(TicketStatus.OPEN);
    private static final Set<TicketStatus> ALLOWED_FROM_REJECTED = Set.of();
    private static final Set<TicketStatus> ALLOWED_FROM_CANCELLED = Set.of();

    @Transactional
    @CacheEvict(cacheNames = "ticket", allEntries = true)
    public TicketResponse createTicket(CreateTicketRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCreatedBy(creator);

        if (request.supportLineId() != null) {
            SupportLine line = supportLineRepository.findById(request.supportLineId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Линия поддержки не найдена: " + request.supportLineId()));
            ticket.setSupportLine(line);

            if (line.getSlaMinutes() != null) {
                ticket.setSlaDeadline(Instant.now().plusSeconds(line.getSlaMinutes() * 60L));
            }
        } else {
            // Если линия не указана, назначаем на первую линию поддержки (минимальный
            // displayOrder)
            SupportLine firstLine = supportLineRepository.findFirstByDeletedAtIsNullOrderByDisplayOrderAsc()
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Дефолтная линия поддержки не установлена"));

            ticket.setSupportLine(firstLine);

            if (firstLine.getSlaMinutes() != null) {
                ticket.setSlaDeadline(Instant.now().plusSeconds(firstLine.getSlaMinutes() * 60L));
            }
        }

        if (request.categoryUserId() != null) {
            Category category = categoryRepository.findById(request.categoryUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Категория не найдена: " + request.categoryUserId()));
            ticket.setCategoryUser(category);
        }

        // Прямое назначение специалисту при создании
        if (request.assignToUserId() != null) {
            User specialist = userRepository.findById(request.assignToUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Специалист не найден: " + request.assignToUserId()));

            if (!specialist.isSpecialist()) {
                throw new IllegalArgumentException("Пользователь не специалист");
            }

            // Проверка что специалист принадлежит выбранной линии (если линия указана)
            if (ticket.getSupportLine() != null &&
                    !ticket.getSupportLine().getSpecialists().contains(specialist)) {
                throw new IllegalArgumentException("Специалист не в выбранной линии поддержки");
            }

            ticket.setAssignedTo(specialist);
            ticket.setStatus(TicketStatus.OPEN);
        }

        Ticket saved = ticketRepository.save(ticket);

        // Записываем начальный статус в историю для учёта времени
        timeTrackingService.recordInitialStatus(saved, creator);

        TicketResponse response = toResponseWithAssignment(saved);

        // Публикуем событие создания через RabbitMQ
        ticketEventPublisher.publishCreated(saved.getId(), userId, response);

        return response;
    }

    /**
     * Get ticket by ID with access check
     */
    @Cacheable(cacheNames = "ticket", key = "#id")
    public TicketResponse getTicketById(Long id, User user) {
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        if (!canAccessTicket(ticket, user)) {
            throw new AccessDeniedException("You don't have access to this ticket");
        }

        return toResponseWithAssignment(ticket);
    }

    /**
     * Хелпер для создания TicketResponse с последним назначением
     */
    private TicketResponse toResponseWithAssignment(Ticket ticket) {
        TicketResponse base = ticketMapper.toResponse(ticket);

        // Получаем последнее назначение для тикета
        var lastAssignment = assignmentRepository.findLatestByTicketId(ticket.getId())
                .map(assignmentMapper::toResponse)
                .orElse(null);

        // Создаём новый response с lastAssignment
        return new TicketResponse(
                base.id(),
                base.title(),
                base.description(),
                base.link1c(),
                base.status(),
                base.priority(),
                base.createdBy(),
                base.assignedTo(),
                base.supportLine(),
                base.categoryUser(),
                base.categorySupport(),
                base.timeSpentSeconds(),
                base.messageCount(),
                base.attachmentCount(),
                base.slaDeadline(),
                base.resolvedAt(),
                base.closedAt(),
                base.createdAt(),
                base.updatedAt(),
                lastAssignment);
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
            return inSameLine && ticket.getAssignedTo() == null;
        }

        return false;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse updateTicket(Long id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

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

        TicketResponse response = ticketMapper.toResponse(updated);

        // Публикуем событие обновления через RabbitMQ
        ticketEventPublisher.publishUpdated(updated.getId(), null, response);

        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);

        // WebSocket: уведомляем об удалении
        broadcastTicketDeleted(id);
    }

    /**
     * Оценить качество обслуживания.
     * Доступно только создателю тикета после его закрытия.
     *
     * @param ticketId ID тикета
     * @param user     пользователь (должен быть создателем)
     * @param rating   оценка 1-5
     * @param feedback отзыв (опционально)
     * @return обновлённый тикет
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#ticketId")
    public TicketResponse rateTicket(Long ticketId, User user, Integer rating, String feedback) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        // Проверка: только создатель может оценить
        if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Только создатель тикета может оставить оценку");
        }

        // Проверка: тикет должен быть закрыт
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalStateException("Оценить можно только закрытый тикет");
        }

        // Проверка: тикет ещё не оценён
        if (ticket.getRating() != null) {
            throw new IllegalStateException("Тикет уже оценён");
        }

        // Валидация оценки
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }

        ticket.setRating(rating);
        ticket.setFeedback(feedback);
        ticket.setRatedAt(Instant.now());
        ticket.touchUpdated();

        Ticket updated = ticketRepository.save(ticket);

        log.info("Тикет {} оценён пользователем {} на {} баллов", ticketId, user.getUsername(), rating);

        TicketResponse response = ticketMapper.toResponse(updated);

        // Публикуем событие оценки через RabbitMQ
        ticketEventPublisher.publishRated(updated.getId(), user.getId(), response);

        // Отправляем уведомление исполнителю об оценке
        if (ticket.getAssignedTo() != null) {
            notificationService.notifyUser(
                    ticket.getAssignedTo().getId(),
                    Notification.rating(ticket.getId(), ticket.getTitle(), rating));
        }

        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse changeStatus(Long id, User user, ChangeStatusRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus newStatus = request.status();

        // Администратор может закрыть тикет принудительно (минуя PENDING_CLOSURE)
        boolean isAdminForceClose = user.isAdmin() &&
                newStatus == TicketStatus.CLOSED &&
                currentStatus != TicketStatus.PENDING_CLOSURE;

        if (!isAdminForceClose && !isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalArgumentException(
                    "Некорректная транзакция статусов: " + currentStatus + " -> " + newStatus);
        }

        // Проверка прав на смену статуса
        boolean isCreator = ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(user.getId());
        boolean isAssignee = ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId());
        boolean isAdmin = user.isAdmin();

        // Логика двухфакторного закрытия:
        // 1. Специалист переводит RESOLVED -> PENDING_CLOSURE (запрос на закрытие)
        // 2. Пользователь (создатель) подтверждает: PENDING_CLOSURE -> CLOSED
        // 3. Пользователь отклоняет: PENDING_CLOSURE -> REOPENED
        // 4. Админ может закрыть принудительно из любого статуса

        if (newStatus == TicketStatus.PENDING_CLOSURE) {
            // Только исполнитель может запросить закрытие
            if (!isAssignee && !isAdmin) {
                throw new AccessDeniedException("Только исполнитель может запросить закрытие тикета");
            }
            ticket.setClosureRequestedBy(user);
            ticket.setClosureRequestedAt(Instant.now());
        } else if (currentStatus == TicketStatus.PENDING_CLOSURE) {
            // Из PENDING_CLOSURE: создатель подтверждает/отклоняет, админ тоже может
            if (!isCreator && !isAdmin) {
                throw new AccessDeniedException("Только создатель тикета может подтвердить или отклонить закрытие");
            }
        } else {
            // Обычная смена статуса - только исполнитель или админ
            if (!isAssignee && !isAdmin) {
                throw new AccessDeniedException("У вас нет права управлять этим тикетом");
            }
        }

        // Записываем смену статуса в историю для учёта времени
        timeTrackingService.recordStatusChange(ticket, newStatus, user, request.comment());

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
            // Очищаем данные запроса на закрытие
            ticket.setClosureRequestedBy(null);
            ticket.setClosureRequestedAt(null);
        } else if (newStatus == TicketStatus.REOPENED) {
            // При переоткрытии сбрасываем время закрытия
            ticket.setClosedAt(null);
            ticket.setResolvedAt(null);
            ticket.setClosureRequestedBy(null);
            ticket.setClosureRequestedAt(null);
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        // Уведомление об изменении статуса
        sendStatusChangeNotification(updated, currentStatus, newStatus);

        TicketResponse response = ticketMapper.toResponse(updated);

        // Публикуем событие смены статуса через RabbitMQ
        ticketEventPublisher.publishStatusChanged(updated.getId(), user.getId(), response);

        return response;
    }

    /**
     * Взять тикет в работу (стать исполнителем)
     */
    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#ticketId")
    public TicketResponse takeTicket(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        User specialist = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверка что пользователь - специалист
        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("Только специалисты могут брать тикеты в работу");
        }

        // Проверка что тикет еще не назначен
        if (ticket.getAssignedTo() != null) {
            throw new IllegalStateException("Тикет уже назначен на " +
                    (ticket.getAssignedTo().getFio() != null ? ticket.getAssignedTo().getFio()
                            : ticket.getAssignedTo().getUsername()));
        }

        // Проверка что специалист в той же линии поддержки
        if (ticket.getSupportLine() != null) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(specialist);
            boolean inSameLine = userLines.stream()
                    .anyMatch(line -> line.getId().equals(ticket.getSupportLine().getId()));

            if (!inSameLine) {
                throw new AccessDeniedException("Вы не входите в линию поддержки этого тикета");
            }
        }

        // Назначаем специалиста
        ticket.setAssignedTo(specialist);

        // Если тикет новый - переводим в статус "В работе"
        TicketStatus oldStatus = ticket.getStatus();
        if (oldStatus == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);

            // Записываем время первой реакции
            if (ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseAt(Instant.now());
            }

            // Записываем смену статуса в историю
            timeTrackingService.recordStatusChange(ticket, TicketStatus.OPEN, specialist, "Тикет взят в работу");
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(updated);

        // Публикуем событие взятия в работу через RabbitMQ
        ticketEventPublisher.publishAssigned(updated.getId(), specialist.getId(), response);

        return response;
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
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse assignToLine(Long id, Long lineId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        SupportLine line = supportLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + lineId));

        ticket.setSupportLine(line);
        ticket.setAssignedTo(null);
        ticket.setStatus(TicketStatus.ESCALATED);

        if (line.getSlaMinutes() != null && ticket.getSlaDeadline() == null) {
            ticket.setSlaDeadline(Instant.now().plusSeconds(line.getSlaMinutes() * 60L));
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(updated);

        // Публикуем событие назначения на линию через RabbitMQ
        ticketEventPublisher.publishAssigned(updated.getId(), null, response);

        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse assignToSpecialist(Long id, Long specialistId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + specialistId));

        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("Пользователь не специалист");
        }

        ticket.setAssignedTo(specialist);

        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = toResponseWithAssignment(updated);
        broadcastTicketUpdate(response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse setUserCategory(Long id, Long categoryId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + categoryId));

        ticket.setCategoryUser(category);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = toResponseWithAssignment(updated);
        broadcastTicketUpdate(response);
        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse setSupportCategory(Long id, Long categoryId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категори я не найдена: " + categoryId));

        ticket.setCategorySupport(category);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = toResponseWithAssignment(updated);
        broadcastTicketUpdate(response);
        return response;
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
            case PENDING_CLOSURE -> ALLOWED_FROM_PENDING_CLOSURE.contains(to);
            case CLOSED -> ALLOWED_FROM_CLOSED.contains(to);
            case REOPENED -> ALLOWED_FROM_REOPENED.contains(to);
            case REJECTED -> ALLOWED_FROM_REJECTED.contains(to);
            case CANCELLED -> ALLOWED_FROM_CANCELLED.contains(to);
        };
    }

    // === WebSocket helpers (deprecated - now using RabbitMQ) ===

    /**
     * Отправить обновление тикета через RabbitMQ
     */
    private void broadcastTicketUpdate(TicketResponse ticket) {
        ticketEventPublisher.publishUpdated(ticket.id(), null, ticket);
        log.debug("RabbitMQ: обновление тикета {}", ticket.id());
    }

    /**
     * Отправить уведомление об удалении тикета через RabbitMQ
     */
    private void broadcastTicketDeleted(Long ticketId) {
        ticketEventPublisher.publishDeleted(ticketId, null);
        log.debug("RabbitMQ: тикет {} удалён", ticketId);
    }
}
