package com.wavii.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassService classService;
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            URI uri = session.getUri();
            Map<String, String> query = parseQuery(uri != null ? uri.getQuery() : null);
            String token = query.get("token");
            String enrollmentIdRaw = query.get("enrollmentId");
            if (token == null || enrollmentIdRaw == null) {
                log.warn("WS class rechazado: faltan parametros");
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

            chatRealtimeBroadcaster.register(chatRealtimeBroadcaster.classRoom(enrollmentId), session);
            session.getAttributes().put("user", user);
            session.getAttributes().put("enrollmentId", enrollmentId);
            log.debug("WS class conectado enrollment={} user={}", shortId(enrollmentId), shortId(user.getId()));
        } catch (Exception e) {
            log.warn("WS class rechazado: {}", e.getMessage());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID enrollmentId = (UUID) session.getAttributes().get("enrollmentId");
        User user = (User) session.getAttributes().get("user");
        if (enrollmentId == null || user == null) {
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        Map<String, Object> saved = classService.sendMessage(
                enrollmentId,
                user,
                payload.get("content") != null ? payload.get("content").toString() : null
        );
        chatRealtimeBroadcaster.broadcast(chatRealtimeBroadcaster.classRoom(enrollmentId), saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRealtimeBroadcaster.unregister(session);
        log.debug("WS class cerrado session={} status={}", session.getId(), status);
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
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                result.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }
        return result;
    }

    private String shortId(UUID id) {
        String raw = id.toString();
        return raw.substring(0, Math.min(8, raw.length()));
    }
}
