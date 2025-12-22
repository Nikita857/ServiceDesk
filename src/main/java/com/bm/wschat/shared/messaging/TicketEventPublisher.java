package com.bm.wschat.shared.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Публикатор событий тикетов в RabbitMQ.
 * Централизованный сервис для отправки всех событий.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Опубликовать событие тикета.
     */
    public void publish(TicketEvent event) {
        String routingKey = "ticket." + event.type().name().toLowerCase();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event);

        log.debug("Published ticket event: type={}, ticketId={}, routingKey={}",
                event.type(), event.ticketId(), routingKey);
    }

    /**
     * Опубликовать событие создания тикета.
     */
    public void publishCreated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.created(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие обновления тикета.
     */
    public void publishUpdated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.updated(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие смены статуса.
     */
    public void publishStatusChanged(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.statusChanged(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие назначения.
     */
    public void publishAssigned(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.assigned(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие нового сообщения.
     */
    public void publishMessageSent(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.messageSent(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие оценки.
     */
    public void publishRated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.rated(ticketId, userId, payload));
    }

    /**
     * Опубликовать событие удаления.
     */
    public void publishDeleted(Long ticketId, Long userId) {
        publish(TicketEvent.deleted(ticketId, userId));
    }
}
