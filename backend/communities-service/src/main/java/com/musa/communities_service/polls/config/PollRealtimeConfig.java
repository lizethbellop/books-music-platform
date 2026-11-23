package com.musa.communities_service.polls.config;

import com.musa.communities_service.polls.realtime.PollWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableScheduling
@EnableWebSocket
public class PollRealtimeConfig implements WebSocketConfigurer {

    private final PollWebSocketHandler handler;
    private final String[] origins;

    public PollRealtimeConfig(
            PollWebSocketHandler handler,
            @Value("${polls.allowed-origins:http://localhost:*}")
            String[] origins
    ) {
        this.handler = handler;
        this.origins = origins;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {
        registry.addHandler(handler, "/api/v1/polls/ws")
                .setAllowedOriginPatterns(origins);
    }

    @Bean
    public ThreadPoolTaskScheduler pollPublisherScheduler() {
        return scheduler("poll-publisher-");
    }

    @Bean
    public ThreadPoolTaskScheduler pollMaintenanceScheduler() {
        return scheduler("poll-maintenance-");
    }

    private ThreadPoolTaskScheduler scheduler(String prefix) {
        var scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(prefix);
        scheduler.setRemoveOnCancelPolicy(true);

        return scheduler;
    }
}