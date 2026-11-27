package com.musa.social.messaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Component
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class SocialNotificationEvents {
    private final NotificationOutbox outbox;
    public SocialNotificationEvents(NotificationOutbox outbox) { this.outbox = outbox; }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void userFollowed(UserFollowedEvent event) {
        outbox.enqueue("social.user.followed", "USER_FOLLOWED", event.followedId(), event.followerId(), event.followerId());
    }
}
