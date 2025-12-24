package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.notification.model.Notification;
import com.bm.wschat.feature.notification.service.NotificationService;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.request.UpdateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.mapper.TicketMapper;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.messaging.TicketEventPublisher;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Сервис для CRUD операций с тикетами.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCrudService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportLineRepository supportLineRepository;
    private final CategoryRepository categoryRepository;
    private final TicketMapper ticketMapper;
    private final NotificationService notificationService;
    private final TicketTimeTrackingService timeTrackingService;
    private final TicketEventPublisher ticketEventPublisher;
    private final TicketQueryService queryService;

    // === Create ===

    @Transactional
    @CacheEvict(cacheNames = "ticket", allEntries = true)
    public TicketResponse createTicket(CreateTicketRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCreatedBy(creator);

        // Линия поддержки
        SupportLine line = resolveSupportLine(request.supportLineId());
        ticket.setSupportLine(line);
        setSlaDeadline(ticket, line);

        // Категория пользователя
        if (request.categoryUserId() != null) {
            Category category = categoryRepository.findById(request.categoryUserId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Категория не найдена: " + request.categoryUserId()));
            ticket.setCategoryUser(category);
        }

        // Прямое назначение специалисту
        if (request.assignToUserId() != null) {
            assignSpecialistOnCreate(ticket, request.assignToUserId());
        }

        Ticket saved = ticketRepository.save(ticket);
        timeTrackingService.recordInitialStatus(saved, creator);

        TicketResponse response = queryService.toResponseWithAssignment(saved);
        ticketEventPublisher.publishCreated(saved.getId(), userId, response);

        log.info("Создан тикет #{} пользователем {}", saved.getId(), creator.getUsername());

        return response;
    }

    // === Update ===

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
        ticketEventPublisher.publishUpdated(updated.getId(), null, response);

        return response;
    }

    // === Delete ===

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);

        ticketEventPublisher.publishDeleted(id, null);
        log.info("Тикет #{} удалён (soft delete)", id);
    }

    // === Rate ===

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#ticketId")
    public TicketResponse rateTicket(Long ticketId, User user, Integer rating, String feedback) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        validateRating(ticket, user, rating);

        ticket.setRating(rating);
        ticket.setFeedback(feedback);
        ticket.setRatedAt(Instant.now());
        ticket.touchUpdated();

        Ticket updated = ticketRepository.save(ticket);

        log.info("Тикет #{} оценён на {} баллов", ticketId, rating);

        TicketResponse response = ticketMapper.toResponse(updated);
        ticketEventPublisher.publishRated(updated.getId(), user.getId(), response);

        // Уведомление исполнителю
        if (ticket.getAssignedTo() != null) {
            notificationService.notifyUser(
                    ticket.getAssignedTo().getId(),
                    Notification.rating(ticket.getId(), ticket.getTitle(), rating));
        }

        return response;
    }

    // === Assignment ===

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse assignToLine(Long id, Long lineId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        SupportLine line = supportLineRepository.findById(lineId)
                .orElseThrow(() -> new EntityNotFoundException("Линия поддержки не найдена: " + lineId));

        ticket.setSupportLine(line);
        ticket.setAssignedToWithTracking(null);
        ticket.setStatus(TicketStatus.ESCALATED);
        setSlaDeadline(ticket, line);

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = ticketMapper.toResponse(updated);
        ticketEventPublisher.publishAssigned(updated.getId(), null, response);

        log.info("Тикет #{} переназначен на линию {}", id, line.getName());

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

        ticket.setAssignedToWithTracking(specialist);

        if (ticket.getStatus() == TicketStatus.NEW) {
            ticket.setStatus(TicketStatus.OPEN);
        }

        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = queryService.toResponseWithAssignment(updated);
        ticketEventPublisher.publishAssigned(updated.getId(), specialistId, response);

        log.info("Тикет #{} назначен на {}", id, specialist.getUsername());

        return response;
    }

    // === Categories ===

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

        TicketResponse response = queryService.toResponseWithAssignment(updated);
        ticketEventPublisher.publishUpdated(updated.getId(), null, response);

        return response;
    }

    @Transactional
    @CacheEvict(cacheNames = "ticket", key = "#id")
    public TicketResponse setSupportCategory(Long id, Long categoryId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + id));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + categoryId));

        ticket.setCategorySupport(category);
        ticket.touchUpdated();
        Ticket updated = ticketRepository.save(ticket);

        TicketResponse response = queryService.toResponseWithAssignment(updated);
        ticketEventPublisher.publishUpdated(updated.getId(), null, response);

        return response;
    }

    // === Helpers ===

    private SupportLine resolveSupportLine(Long lineId) {
        if (lineId != null) {
            return supportLineRepository.findById(lineId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Линия поддержки не найдена: " + lineId));
        }
        return supportLineRepository.findFirstByDeletedAtIsNullOrderByDisplayOrderAsc()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Дефолтная линия поддержки не установлена"));
    }

    private void setSlaDeadline(Ticket ticket, SupportLine line) {
        if (line.getSlaMinutes() != null && ticket.getSlaDeadline() == null) {
            ticket.setSlaDeadline(Instant.now().plusSeconds(line.getSlaMinutes() * 60L));
        }
    }

    private void assignSpecialistOnCreate(Ticket ticket, Long specialistId) {
        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Специалист не найден: " + specialistId));

        if (!specialist.isSpecialist()) {
            throw new IllegalArgumentException("Пользователь не специалист");
        }

        if (ticket.getSupportLine() != null &&
                !ticket.getSupportLine().getSpecialists().contains(specialist)) {
            throw new IllegalArgumentException("Специалист не в выбранной линии поддержки");
        }

        ticket.setAssignedToWithTracking(specialist);
        ticket.setStatus(TicketStatus.OPEN);
    }

    private void validateRating(Ticket ticket, User user, Integer rating) {
        if (ticket.getCreatedBy() == null || !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Только создатель тикета может оставить оценку");
        }

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalStateException("Оценить можно только закрытый тикет");
        }

        if (ticket.getRating() != null) {
            throw new IllegalStateException("Тикет уже оценён");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Оценка должна быть от 1 до 5");
        }
    }
}
