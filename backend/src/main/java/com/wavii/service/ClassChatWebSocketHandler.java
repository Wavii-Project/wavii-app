package com.wavii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.model.ClassEnrollment;
import com.wavii.model.User;
import com.wavii.repository.ClassEnrollmentRepository;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassService classService;

    private final Map<UUID, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionRooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        Map<String, String> query = parseQuery(uri != null ? uri.getQuery() : null);
        String token = query.get("token");
        String enrollmentIdRaw = query.get("enrollmentId");
        if (token == null || enrollmentIdRaw == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        User user = authenticate(token);
        UUID enrollmentId = UUID.fromString(enrollmentIdRaw);
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada"));
        if (!enrollment.getTeacher().getId().equals(user.getId())
                && !enrollment.getStudent().getId().equals(user.getId())) {
            throw new IllegalArgumentException("No tienes acceso a esta clase");
        }
        String status = enrollment.getPaymentStatus() != null ? enrollment.getPaymentStatus().trim().toLowerCase() : "";
        boolean canChat = "accepted".equals(status)
                || "paid".equals(status)
                || "scheduled".equals(status)
                || "completed".equals(status)
                || "refund_requested".equals(status);
        if (!canChat) {
            throw new IllegalArgumentException("La clase todavia no esta disponible para el chat");
        }

        rooms.computeIfAbsent(enrollmentId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionRooms.put(session.getId(), enrollmentId);
        session.getAttributes().put("user", user);
        session.getAttributes().put("enrollmentId", enrollmentId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID enrollmentId = (UUID) session.getAttributes().get("enrollmentId");
        User user = (User) session.getAttributes().get("user");
        if (enrollmentId == null || user == null) {
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String content = payload.get("content") != null ? payload.get("content").toString() : "";
        Map<String, Object> saved = classService.sendMessage(enrollmentId, user, content);
        broadcast(enrollmentId, saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID enrollmentId = sessionRooms.remove(session.getId());
        if (enrollmentId == null) {
            return;
        }
        Map<String, WebSocketSession> room = rooms.get(enrollmentId);
        if (room != null) {
            room.remove(session.getId());
            if (room.isEmpty()) {
                rooms.remove(enrollmentId);
            }
        }
    }

    private void broadcast(UUID enrollmentId, Map<String, Object> payload) throws Exception {
        Map<String, WebSocketSession> room = rooms.get(enrollmentId);
        if (room == null) {
            return;
        }
        String json = objectMapper.writeValueAsString(payload);
        for (WebSocketSession ws : room.values()) {
            if (ws.isOpen()) {
                ws.sendMessage(new TextMessage(json));
            }
        }
    }

    private User authenticate(String token) {
        String raw = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtService.extractEmail(raw);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!jwtService.isTokenValid(raw, user)) {
            throw new IllegalArgumentException("Token invalido");
        }
        return user;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new ConcurrentHashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }
}
