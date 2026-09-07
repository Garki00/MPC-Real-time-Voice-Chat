package com.mpc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpc.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC signaling server over raw WebSocket.
 * Messages are JSON: { type, from, to, channelId, payload }
 * Types: offer, answer, ice-candidate, join-channel, leave-channel
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    // userId -> session
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = authenticate(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        session.getAttributes().put("userId", userId);
        sessions.put(userId, session);
        log.debug("Signal WS connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromId = (Long) session.getAttributes().get("userId");
        if (fromId == null) return;

        Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) msg.get("type");
        Object toRaw = msg.get("to");

        if (toRaw != null) {
            Long toId = Long.parseLong(toRaw.toString());
            WebSocketSession target = sessions.get(toId);
            if (target != null && target.isOpen()) {
                msg.put("from", fromId);
                target.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            log.debug("Signal WS disconnected: userId={}", userId);
        }
    }

    private Long authenticate(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                String token = kv[1];
                if (jwtUtil.isValid(token)) return jwtUtil.getUserId(token);
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (Exception ignored) {}
    }
}
