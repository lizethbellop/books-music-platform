package com.musa.chat.config;
import com.musa.chat.realtime.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocket
@EnableScheduling
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final String[] origins;
    public ChatWebSocketConfig(ChatWebSocketHandler handler,
            @Value("${musa.chat.allowed-origins:http://localhost:3000}") String[] origins) {
        this.handler = handler; this.origins = origins;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/chat/ws").setAllowedOriginPatterns(origins);
    }
}
