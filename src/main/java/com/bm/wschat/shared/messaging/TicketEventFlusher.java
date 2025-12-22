package com.bm.wschat.shared.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Слушатель транзакций для отправки агрегированных событий после коммита.
 * 
 * Логика:
 * 1. Во время транзакции события накапливаются в TicketEventAggregator
 * 2. После COMMIT транзакции — публикуем события с @TransactionalEventListener
 * (невозможно)
 * 
 * Альтернативный подход: вызываем flush явно в конце @Transactional метода.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventFlusher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Отправить все агрегированные события в RabbitMQ.
     * Вызывается явно в конце транзакции или через AOP.
     */
    public void flush(TicketEventAggregator aggregator) {
        if (!aggregator.hasEvents()) {
            return;
        }

        int count = aggregator.size();

        for (TicketEvent event : aggregator.getEvents()) {
            String routingKey = "ticket." + event.type().name().toLowerCase();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    routingKey,
                    event);

            log.debug("Flushed aggregated event: type={}, ticketId={}",
                    event.type(), event.ticketId());
        }

        aggregator.clear();
        log.debug("Flushed {} aggregated events", count);
    }
}
