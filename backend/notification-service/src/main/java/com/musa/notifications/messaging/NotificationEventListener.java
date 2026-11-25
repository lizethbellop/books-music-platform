package com.musa.notifications.messaging;

import com.musa.notifications.config.RabbitMQConfig;
import com.musa.notifications.dto.NotificationEvent;
import com.musa.notifications.service.NotificationService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class NotificationEventListener {

    private final NotificationService service;

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    public NotificationEventListener(NotificationService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void receive(Message message) {
        if (message.getBody().length > 16384) {
            throw new AmqpRejectAndDontRequeueException("Evento demasiado grande.");
        }
        NotificationEvent event;

        try {
            event = jsonMapper.readValue(
                    message.getBody(),
                    NotificationEvent.class
            );
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "El evento no contiene un JSON válido."
            );
        }

        if (event == null) {
            throw new AmqpRejectAndDontRequeueException(
                    "El evento está vacío."
            );
        }

        if (event.type() != null) {
            String expectedKey = switch (event.type()) {
                case CHAT_MESSAGE_CREATED -> "chat.message.created";
                case USER_FOLLOWED -> "social.user.followed";
                case POST_LIKED -> "social.post.liked";
                case POST_COMMENTED -> "social.post.commented";
                case GROUP_MEMBER_ADDED -> "chat.group.member.added";
                case GROUP_MEMBER_REMOVED -> "chat.group.member.removed";
                case GROUP_ADMIN_CHANGED -> "chat.group.admin.changed";
                case POST_PUBLISHED -> "social.post.published";
            };
            if (!expectedKey.equals(message.getMessageProperties().getReceivedRoutingKey())) {
                throw new AmqpRejectAndDontRequeueException("Tipo de evento y routing key incompatibles.");
            }
            if (event.type() == com.musa.notifications.entity.NotificationType.USER_FOLLOWED
                    && !java.util.Objects.equals(event.actorId(), event.targetId())) {
                throw new AmqpRejectAndDontRequeueException("Destino de seguimiento incompatible.");
            }
        }
        try {
            service.process(event);
        } catch (ConstraintViolationException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "El evento no cumple el contrato."
            );
        }
    }
}