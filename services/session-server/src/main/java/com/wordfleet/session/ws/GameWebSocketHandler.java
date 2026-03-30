package com.wordfleet.session.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordfleet.session.service.GameRoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final GameRoomManager roomManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public GameWebSocketHandler(GameRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = asString(session.getAttributes().get("roomId"));
        String userId = asString(session.getAttributes().get("userId"));
        if (roomId == null || userId == null) {
            safeClose(session);
            return;
        }
        roomManager.connect(roomId, userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String roomId = asString(session.getAttributes().get("roomId"));
        String userId = asString(session.getAttributes().get("userId"));
        if (roomId == null || userId == null) {
            safeClose(session);
            return;
        }

        try {
            JsonNode root = mapper.readTree(message.getPayload());
            String type = root.path("type").asText("");

            switch (type) {
                case "SUBMIT_WORD" -> {
                    String word = root.path("payload").path("word").asText("");
                    roomManager.submitWord(roomId, userId, word);
                }
                case "START_NEW_GAME" -> roomManager.startNewGame(roomId, userId);
                case "PING" -> session.sendMessage(new TextMessage(mapper.writeValueAsString(
                        Map.of("type", "PONG", "payload", Map.of("ts", System.currentTimeMillis())))));
                default -> session.sendMessage(new TextMessage(mapper.writeValueAsString(
                        Map.of("type", "ERROR", "payload", Map.of("message", "Unknown message type")))));
            }
        } catch (Exception ex) {
            log.debug("Invalid websocket payload: {}", ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = asString(session.getAttributes().get("roomId"));
        String userId = asString(session.getAttributes().get("userId"));
        if (roomId != null && userId != null) {
            roomManager.disconnect(roomId, userId, session.getId());
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private void safeClose(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception ignored) {
        }
    }
}
