package com.musa.notifications.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;

@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "musa.events";
    public static final String NOTIFICATIONS_QUEUE = "musa.notifications";

    public static final String DEAD_LETTER_EXCHANGE =
            "musa.notifications.dlx";

    public static final String DEAD_LETTER_QUEUE =
            "musa.notifications.dlq";

    public static final String DEAD_LETTER_ROUTING_KEY =
            "notifications.failed";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE).build();
    }

    @Bean
    public Binding chatNotificationsBinding(
            @Qualifier("notificationsQueue") Queue queue,
            TopicExchange eventsExchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(eventsExchange)
                .with("chat.message.created");
    }

    @Bean
    public Binding socialNotificationsBinding(
            @Qualifier("notificationsQueue") Queue queue,
            TopicExchange eventsExchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(eventsExchange)
                .with("social.#");
    }

    @Bean
    public Binding groupNotificationsBinding(@Qualifier("notificationsQueue") Queue queue,
            TopicExchange eventsExchange) {
        return BindingBuilder.bind(queue).to(eventsExchange).with("chat.group.#");
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(
            @Qualifier("deadLetterQueue") Queue queue,
            DirectExchange deadLetterExchange
    ) {
        return BindingBuilder
                .bind(queue)
                .to(deadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageRecoverer notificationMessageRecoverer() {
        return new RejectAndDontRequeueRecoverer(
                "Evento no procesado después de los reintentos."
        );
    }
}