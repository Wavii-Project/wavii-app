package com.wavii.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.PostResponse;
import com.wavii.model.Forum;
import com.wavii.model.User;
import com.wavii.repository.ForumMembershipRepository;
import com.wavii.repository.ForumRepository;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class ForumChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ForumRepository forumRepository;
    private final ForumMembershipRepository membershipRepository;
    private final ForumService forumService;

    private final Map<UUID, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionRooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        Map<String, String> query = parseQuery(uri != null ? uri.getQuery() : null);
        String token = query.get("token");
        String forumIdRaw = query.get("forumId");
        if (token == null || forumIdRaw == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        User user = authenticate(token);
        UUID forumId = UUID.fromString(forumIdRaw);
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("Comunidad no encontrada"));
        if (!membershipRepository.existsByForumAndUser(forum, user)) {
            throw new IllegalArgumentException("Debes unirte a la comunidad");
        }

        rooms.computeIfAbsent(forumId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionRooms.put(session.getId(), forumId);
        session.getAttributes().put("user", user);
        session.getAttributes().put("forumId", forumId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID forumId = (UUID) session.getAttributes().get("forumId");
        User user = (User) session.getAttributes().get("user");
        if (forumId == null || user == null) {
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String content = payload.get("content") != null ? payload.get("content").toString() : "";
        PostResponse saved = forumService.createPost(forumId, new CreatePostRequest(content), user);
        broadcast(forumId, saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID forumId = sessionRooms.remove(session.getId());
        if (forumId == null) {
            return;
        }
        Map<String, WebSocketSession> room = rooms.get(forumId);
        if (room != null) {
            room.remove(session.getId());
            if (room.isEmpty()) {
                rooms.remove(forumId);
            }
        }
    }

    private void broadcast(UUID forumId, PostResponse payload) throws Exception {
        Map<String, WebSocketSession> room = rooms.get(forumId);
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
