package com.bm.wschat.shared.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация RabbitMQ для событий тикетов.
 */
@Configuration
public class RabbitMQConfig {


    /**
     * Внутренние маршруты для брокера
     */
    public static final String EXCHANGE_NAME = "servicedesk.events";
    public static final String TICKET_QUEUE = "servicedesk.ticket.events";
    public static final String ROUTING_KEY = "ticket.#";

    /**
     * Маршруты для отправки уведомлений в Телеграмм
     */
    public static final String TELEGRAM_QUEUE = "servicedesk.telegram.notifications";
    public static final String TELEGRAM_ROUTING_KEY = "ticket.#";

//    ================= Internal queue ================
    @Bean
    public TopicExchange ticketExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue ticketQueue() {
        return QueueBuilder.durable(TICKET_QUEUE).build();
    }

    @Bean
    public Binding ticketBinding(Queue ticketQueue, TopicExchange ticketExchange) {
        return BindingBuilder
                .bind(ticketQueue)
                .to(ticketExchange)
                .with(ROUTING_KEY);
    }

//    ================ TG queue ===============

    @Bean
    public Queue telegramQueue() {
        return QueueBuilder.durable(TELEGRAM_QUEUE).build();
    }

    @Bean
    public Binding telegramBinding(Queue telegramQueue, TopicExchange ticketExchange) {
        return BindingBuilder
                .bind(telegramQueue)
                .to(ticketExchange)
                .with(TELEGRAM_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
