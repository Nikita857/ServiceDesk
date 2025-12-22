package com.bm.wschat.shared.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация RabbitMQ для событий тикетов.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "servicedesk.events";
    public static final String TICKET_QUEUE = "servicedesk.ticket.events";
    public static final String ROUTING_KEY = "ticket.#";

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
