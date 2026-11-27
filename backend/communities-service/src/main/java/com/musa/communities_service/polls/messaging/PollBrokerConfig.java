package com.musa.communities_service.polls.messaging;

import com.musa.communities_service.polls.realtime.PollWebSocketHandler;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
@ConditionalOnProperty(
        name = "polls.events.enabled",
        havingValue = "true"
)
public class PollBrokerConfig {

    @Bean
    public FanoutExchange pollExchange() {
        return new FanoutExchange("musa.polls", true, false);
    }

    @Bean
    public Queue pollLiveQueue() {
        return QueueBuilder.nonDurable()
                .exclusive()
                .autoDelete()
                .withArgument("x-max-length", 256)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Binding pollBinding(
            @Qualifier("pollLiveQueue") Queue queue,
            FanoutExchange pollExchange
    ) {
        return BindingBuilder.bind(queue).to(pollExchange);
    }

    @Bean
    public RabbitTemplate pollRabbitTemplate(
            CachingConnectionFactory factory
    ) {
        factory.setPublisherConfirmType(
                CachingConnectionFactory.ConfirmType.CORRELATED
        );

        return new RabbitTemplate(factory);
    }

    @Bean
    public SimpleMessageListenerContainer pollListener(
            CachingConnectionFactory factory,
            @Qualifier("pollLiveQueue") Queue queue,
            PollWebSocketHandler sockets
    ) {
        var container = new SimpleMessageListenerContainer(factory);

        container.setQueues(queue);
        container.setConcurrentConsumers(1);
        container.setPrefetchCount(16);
        container.setDefaultRequeueRejected(false);

        container.setMessageListener((MessageListener) message -> {
            if (message.getBody().length > 64) {
                return;
            }

            try {
                UUID pollId = UUID.fromString(
                        new String(
                                message.getBody(),
                                StandardCharsets.UTF_8
                        )
                );

                sockets.invalidate(pollId);
            } catch (IllegalArgumentException ignored) {
                // Un aviso inválido no se reencola indefinidamente.
            }
        });

        return container;
    }
}