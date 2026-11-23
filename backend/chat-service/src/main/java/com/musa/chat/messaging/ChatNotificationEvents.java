package com.musa.chat.messaging;
import com.musa.chat.service.MessageCreatedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Component
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class ChatNotificationEvents {
    private final NotificationOutbox outbox;
    public ChatNotificationEvents(NotificationOutbox outbox) { this.outbox = outbox; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void messageCreated(MessageCreatedEvent event) {
        for (var recipient : event.participantIds()) {
            outbox.enqueue("chat.message.created", "CHAT_MESSAGE_CREATED", recipient,
                    event.message().senderId(), event.message().conversationId());
        }
    }
}
