package com.example.vacancyscraper.config;

import com.example.vacancyscraper.websocket.ParseJobWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ParseJobWebSocketHandler parseJobWebSocketHandler;

    public WebSocketConfig(ParseJobWebSocketHandler parseJobWebSocketHandler) {
        this.parseJobWebSocketHandler = parseJobWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(parseJobWebSocketHandler, "/ws/parse")
                .setAllowedOriginPatterns("*");
    }
}
