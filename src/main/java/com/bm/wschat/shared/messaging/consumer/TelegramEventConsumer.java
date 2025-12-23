package com.bm.wschat.shared.messaging.consumer;

import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserActivityStatusRepository;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.messaging.producer.TelegramProducer;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.shared.messaging.config.RabbitMQConfig;
import com.bm.wschat.shared.messaging.event.TicketEvent;
import com.bm.wschat.shared.messaging.template.TelegramMessageTemplate;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramEventConsumer {

    private final TelegramProducer telegramProducer;
    private final TicketRepository ticketRepository;
    private final TelegramMessageTemplate messageTemplate;
    private final UserRepository userRepository;
    private final UserActivityStatusRepository userActivityStatusRepository;

    @RabbitListener(queues = RabbitMQConfig.TELEGRAM_QUEUE)
    @Transactional(readOnly = true)
    public void handleTicketEvent(TicketEvent event) {
        log.debug("Received ticket event for Telegram: {}", event);

        try {
            Ticket ticket = ticketRepository.findById(event.ticketId()).orElse(null);
            if (ticket == null) {
                log.warn("Ticket not found for event: {}", event);
                return;
            }

            switch (event.type()) {
                case CREATED -> handleCreated(ticket);
                case ASSIGNED -> handleAssigned(ticket);
                case MESSAGE_SENT -> handleMessageSent(ticket, event);
                case STATUS_CHANGED -> handleStatusChanged(ticket);
                case RATED -> handleRated(ticket);
            }

        } catch (Exception e) {
            log.error("Error handling ticket event for Telegram notification", e);
        }
    }

    private void handleCreated(Ticket ticket) {
        // Уведомление в канал линии поддержки
        if (ticket.getSupportLine() != null && ticket.getSupportLine().getTelegramChatId() != null) {
            String msg = messageTemplate.buildCreatedMessage(ticket);
            telegramProducer.sendMessage(ticket.getSupportLine().getTelegramChatId(), msg);
        }
    }

    private void handleAssigned(Ticket ticket) {
        // Уведомить исполнителя, что ему назначили тикет
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getTelegramId() != null) {
            String msg = messageTemplate.buildAssignedMessage(ticket);
            telegramProducer.sendMessage(ticket.getAssignedTo().getTelegramId(), msg);
        }

        // Уведомить создателя, что заявку взяли (если инициатор назначения - не он сам)
        if (ticket.getCreatedBy().getTelegramId() != null) {
            String msg = messageTemplate.buildTakenInWorkMessage(ticket);
            telegramProducer.sendMessage(ticket.getCreatedBy().getTelegramId(), msg);
        }
    }

    private void handleMessageSent(Ticket ticket, TicketEvent event) {
        Long senderId = event.userId();

        // Если пишет Исполнитель -> уведомляем Автора
        if (ticket.getAssignedTo() != null && Objects.equals(ticket.getAssignedTo().getId(), senderId)) {
            notifyUser(ticket.getCreatedBy(), ticket, "💬 *Новое сообщение от поддержки ()*");
        }
        // Если пишет Автор -> уведомляем Исполнителя
        else if (Objects.equals(ticket.getCreatedBy().getId(), senderId)) {
             if (ticket.getAssignedTo() != null) {
                 notifyUser(ticket.getAssignedTo(), ticket, "💬 *Новое сообщение от пользователя*");
             }
        }
        // Если пишет кто-то третий -> уведомляем обоих
        else {
             notifyUser(ticket.getCreatedBy(), ticket, "💬 *Новое сообщение в заявке*");
             if (ticket.getAssignedTo() != null) {
                 notifyUser(ticket.getAssignedTo(), ticket, "💬 *Новое сообщение в заявке*");
             }
        }
    }

    private void handleStatusChanged(Ticket ticket) {
        // Если тикет закрыт -> особое уведомление
        if (ticket.getStatus() == TicketStatus.RESOLVED|| ticket.getStatus() == TicketStatus.CLOSED) {
             notifyUser(ticket.getCreatedBy(), messageTemplate.buildClosedMessage(ticket));
        } else {
             notifyUser(ticket.getCreatedBy(), messageTemplate.buildStatusChangedMessage(ticket));
        }
    }

    private void handleRated(Ticket ticket) {
        // Уведомляем специалиста об оценке
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getTelegramId() != null) {
            String msg = messageTemplate.buildRatedMessage(ticket);
            telegramProducer.sendMessage(ticket.getAssignedTo().getTelegramId(), msg);
        }
    }

    private void notifyUser(User user, String message) {
        if (isOnline(user) && user.getTelegramId() != null) {
            telegramProducer.sendMessage(user.getTelegramId(), message);
        }
    }

    private void notifyUser(User user, Ticket ticket, String title) {
        if (isOnline(user) && user.getTelegramId() != null) {
            String msg = String.format("%s\nЗаявка #%d: %s", title, ticket.getId(), ticket.getTitle());
            telegramProducer.sendMessage(user.getTelegramId(), msg);
        }
    }

    private boolean isOnline(User user) {
        UserActivityStatusEntity status = userActivityStatusRepository.findByUserId(user.getId()).orElseThrow(
                () -> new EntityNotFoundException("Невозможно определить статус пользователя")
        );
        return status.getStatus() == UserActivityStatus.UNAVAILABLE ||
                status.getStatus() == UserActivityStatus.OFFLINE ||
                status.getStatus() == UserActivityStatus.TECHNICAL_ISSUE;
    }
}
