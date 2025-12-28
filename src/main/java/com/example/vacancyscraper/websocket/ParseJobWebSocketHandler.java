package com.example.vacancyscraper.websocket;

import com.example.vacancyscraper.dto.ParseJobResponse;
import com.example.vacancyscraper.model.ParseJob;
import com.example.vacancyscraper.service.ParseJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ParseJobWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ParseJobWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public ParseJobWebSocketHandler(ParseJobService parseJobService, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        parseJobService.registerListener(this::broadcastCompletion);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket closed: {} ({})", session.getId(), status);
    }

    private void broadcastCompletion(ParseJob job) {
        if (!job.isFinished()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(ParseJobResponse.from(job));
            TextMessage message = new TextMessage(payload);
            sessions.forEach(session -> sendSilently(session, message));
        } catch (Exception e) {
            log.warn("Failed to serialize job {} for WebSocket broadcast: {}", job.getId(), e.getMessage());
        }
    }

    private void sendSilently(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(message);
        } catch (IOException e) {
            log.debug("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }
}
