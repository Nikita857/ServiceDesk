package com.bm.wschat.shared.messaging.consumer;

import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserActivityStatusRepository;
import com.bm.wschat.shared.messaging.producer.TelegramProducer;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.shared.messaging.config.RabbitMQConfig;
import com.bm.wschat.shared.messaging.event.TicketEvent;
import com.bm.wschat.shared.messaging.template.TelegramMessageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Консьюмер событий тикетов для отправки уведомлений в Telegram.
 * 
 * Логика уведомлений:
 * - Уведомляем пользователя когда он OFFLINE/UNAVAILABLE (не в приложении)
 * - Не уведомляем когда AVAILABLE/BUSY (активен в приложении, видит сообщения)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramEventConsumer {

    private final TelegramProducer telegramProducer;
    private final TicketRepository ticketRepository;
    private final TelegramMessageTemplate messageTemplate;
    private final UserActivityStatusRepository userActivityStatusRepository;

    @RabbitListener(queues = RabbitMQConfig.TELEGRAM_QUEUE)
    @Transactional(readOnly = true)
    public void handleTicketEvent(TicketEvent event) {
        log.debug("Received ticket event for Telegram: type={}, ticketId={}", event.type(), event.ticketId());

        try {
            Ticket ticket = ticketRepository.findById(event.ticketId()).orElse(null);
            if (ticket == null) {
                log.warn("Ticket not found for event: {}", event.ticketId());
                return;
            }

            switch (event.type()) {
                case CREATED -> handleCreated(ticket);
                case ASSIGNED -> handleAssigned(ticket);
                case STATUS_CHANGED -> handleStatusChanged(ticket);
                default -> log.trace("Ignoring event type {} for Telegram", event.type());
            }

        } catch (Exception e) {
            log.error("Error handling ticket event for Telegram: ticketId={}, error={}",
                    event.ticketId(), e.getMessage(), e);
        }
    }

    /**
     * Новая заявка — уведомляем канал линии поддержки
     */
    private void handleCreated(Ticket ticket) {
        if (ticket.getSupportLine() != null && ticket.getSupportLine().getTelegramChatId() != null) {
            String msg = messageTemplate.buildCreatedMessage(ticket);
            telegramProducer.sendMessage(ticket.getSupportLine().getTelegramChatId(), msg);
            log.debug("Sent CREATED notification to support line chat: ticketId={}", ticket.getId());
        }
    }

    /**
     * Назначение — уведомляем исполнителя и автора
     */
    private void handleAssigned(Ticket ticket) {
        // Уведомить исполнителя
        if (ticket.getAssignedTo() != null) {
            notifyUserIfOffline(ticket.getAssignedTo(),
                    messageTemplate.buildAssignedMessage(ticket));
        }

        // Уведомить автора что заявку взяли
        if (ticket.getCreatedBy() != null &&
                !Objects.equals(ticket.getCreatedBy().getId(),
                        ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)) {
            notifyUserIfOffline(ticket.getCreatedBy(),
                    messageTemplate.buildTakenInWorkMessage(ticket));
        }
    }

    /**
     * Смена статуса — уведомляем автора
     */
    private void handleStatusChanged(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            notifyUserIfOffline(ticket.getCreatedBy(), messageTemplate.buildClosedMessage(ticket));
        } else {
            notifyUserIfOffline(ticket.getCreatedBy(), messageTemplate.buildStatusChangedMessage(ticket));
        }
    }

    // === Helper methods ===

    /**
     * Отправить уведомление пользователю если он НЕ активен в приложении.
     * Логика: если пользователь OFFLINE или UNAVAILABLE — отправляем в Telegram.
     */
    private void notifyUserIfOffline(User user, String message) {
        if (user == null || user.getTelegramId() == null) {
            return;
        }

        if (shouldNotifyViaTelegram(user)) {
            telegramProducer.sendMessage(user.getTelegramId(), message);
            log.debug("Sent Telegram notification to user {}", user.getId());
        } else {
            log.trace("User {} is online, skipping Telegram notification", user.getId());
        }
    }

    /**
     * Проверяет нужно ли отправлять уведомление в Telegram.
     * Отправляем когда пользователь НЕ активен в приложении.
     */
    private boolean shouldNotifyViaTelegram(User user) {
        Optional<UserActivityStatusEntity> statusOpt = userActivityStatusRepository.findByUserId(user.getId());

        if (statusOpt.isEmpty()) {
            // Нет статуса — считаем что оффлайн, уведомляем
            return true;
        }

        UserActivityStatus status = statusOpt.get().getStatus();

        // Уведомляем когда пользователь НЕ в приложении
        return status == UserActivityStatus.OFFLINE ||
                status == UserActivityStatus.UNAVAILABLE ||
                status == UserActivityStatus.TECHNICAL_ISSUE;
    }
}
