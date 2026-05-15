package com.wavii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRealtimeBroadcaster {

    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    public void register(String roomId, WebSocketSession session) {
        rooms.computeIfAbsent(roomId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionRooms.put(session.getId(), roomId);
    }

    public void unregister(WebSocketSession session) {
        String roomId = sessionRooms.remove(session.getId());
        if (roomId == null) {
            return;
        }
        Map<String, WebSocketSession> room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        room.remove(session.getId());
        if (room.isEmpty()) {
            rooms.remove(roomId);
        }
    }

    public void broadcast(String roomId, Object payload) {
        Map<String, WebSocketSession> room = rooms.get(roomId);
        if (room == null || room.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : room.values()) {
                if (!session.isOpen()) {
                    continue;
                }
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("Error emitiendo mensaje en {}: {}", roomId, e.getMessage());
        }
    }

    public String directRoom(UUID left, UUID right) {
        String a = left.toString();
        String b = right.toString();
        return "direct:" + (a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a);
    }

    public String classRoom(UUID enrollmentId) {
        return "class:" + enrollmentId;
    }

    public String forumRoom(UUID forumId) {
        return "forum:" + forumId;
    }
}
