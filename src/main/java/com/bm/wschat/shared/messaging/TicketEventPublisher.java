package com.bm.wschat.shared.messaging;

import com.bm.wschat.shared.messaging.config.RabbitMQConfig;
import com.bm.wschat.shared.messaging.event.TicketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Публикатор событий тикетов в RabbitMQ.
 * 
 * Поддерживает два режима:
 * 1. Агрегированный (в контексте HTTP-запроса) — события накапливаются и
 * отправляются после коммита
 * 2. Немедленный (вне контекста запроса) — события отправляются сразу
 * 
 * Используется ObjectProvider для ленивой инъекции RequestScope бина.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectProvider<TicketEventAggregator> aggregatorProvider;
    private final TicketEventFlusher eventFlusher;

    /**
     * Опубликовать событие тикета.
     * В контексте HTTP-запроса — добавляет в агрегатор.
     * Вне контекста — отправляет немедленно.
     */
    public void publish(TicketEvent event) {

        if (RequestContextHolder.getRequestAttributes() == null) {
            publishImmediately(event);
            return;
        }

        try {
            // Пытаемся получить агрегатор (работает только в RequestScope)
            TicketEventAggregator aggregator = aggregatorProvider.getIfAvailable();
            if (aggregator != null) {
                aggregator.addEvent(event);
                log.trace("Event added to aggregator: type={}, ticketId={}",
                        event.type(), event.ticketId());
            } else {
                // Вне HTTP-запроса — отправляем сразу
                publishImmediately(event);
            }
        } catch (Exception e) {
            // Fallback — отправляем сразу если что-то пошло не так
            log.warn("Failed to aggregate event, publishing immediately: {}", e.getMessage());
            publishImmediately(event);
        }
    }

    /**
     * Отправить событие немедленно (обход агрегатора).
     * Используется для критичных событий или вне HTTP-контекста.
     */
    public void publishImmediately(TicketEvent event) {
        String routingKey = "ticket." + event.type().name().toLowerCase();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                routingKey,
                event);

        log.debug("Published ticket event immediately: type={}, ticketId={}, routingKey={}",
                event.type(), event.ticketId(), routingKey);
    }

    /**
     * Принудительно отправить все накопленные события.
     * Вызывается в конце транзакции через AOP или вручную.
     */
    public void flush() {
        // Проверяем наличие RequestScope — без него агрегатор недоступен
        if (RequestContextHolder.getRequestAttributes() == null) {
            return;
        }

        try {
            TicketEventAggregator aggregator = aggregatorProvider.getIfAvailable();
            if (aggregator != null && aggregator.hasEvents()) {
                eventFlusher.flush(aggregator);
            }
        } catch (Exception e) {
            log.warn("Failed to flush events: {}", e.getMessage());
        }
    }

    // === Convenience methods ===

    public void publishCreated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.created(ticketId, userId, payload));
    }

    public void publishUpdated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.updated(ticketId, userId, payload));
    }

    public void publishStatusChanged(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.statusChanged(ticketId, userId, payload));
    }

    public void publishAssigned(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.assigned(ticketId, userId, payload));
    }

    public void publishMessageSent(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.messageSent(ticketId, userId, payload));
    }

    public void publishRated(Long ticketId, Long userId, Object payload) {
        publish(TicketEvent.rated(ticketId, userId, payload));
    }

    public void publishDeleted(Long ticketId, Long userId) {
        publish(TicketEvent.deleted(ticketId, userId));
    }

    /**
     * Публикует событие о создании назначения для получателя.
     * Отправляется персонально назначенному пользователю.
     */
    public void publishAssignmentCreated(Long ticketId, Long toUserId, Object payload) {
        publish(TicketEvent.assignmentCreated(ticketId, toUserId, payload));
    }

    /**
     * Публикует событие об отклонении назначения.
     * Отправляется пользователю, создавшему назначение (fromUser).
     */
    public void publishAssignmentRejected(Long ticketId, Long fromUserId, Object payload) {
        publish(TicketEvent.assignmentRejected(ticketId, fromUserId, payload));
    }
}
