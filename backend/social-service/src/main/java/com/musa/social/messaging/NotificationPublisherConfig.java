package com.musa.social.messaging;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "notifications.events.enabled", havingValue = "true")
public class NotificationPublisherConfig {
    @Bean public TopicExchange notificationEventsExchange() { return new TopicExchange("musa.events", true, false); }
}
