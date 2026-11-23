package com.musa.chat.messaging;
import com.musa.chat.service.GroupChangedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Component
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class GroupNotificationEvents {
    private final NotificationOutbox outbox;
    public GroupNotificationEvents(NotificationOutbox outbox) { this.outbox = outbox; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void changed(GroupChangedEvent event) {
        String type;
        String routing;
        switch (event.action()) {
            case CREATED, MEMBER_ADDED -> { type = "GROUP_MEMBER_ADDED"; routing = "chat.group.member.added"; }
            case MEMBER_REMOVED -> { type = "GROUP_MEMBER_REMOVED"; routing = "chat.group.member.removed"; }
            case OWNER_CHANGED -> { type = "GROUP_ADMIN_CHANGED"; routing = "chat.group.admin.changed"; }
            default -> { return; }
        }
        for (var recipient : event.affectedUserIds())
            outbox.enqueue(routing, type, recipient, event.actorId(), event.conversationId());
    }
}
