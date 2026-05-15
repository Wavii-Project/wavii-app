package com.wavii.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.PostResponse;
import com.wavii.model.Forum;
import com.wavii.model.User;
import com.wavii.repository.ForumMembershipRepository;
import com.wavii.repository.ForumRepository;
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
public class ForumChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ForumRepository forumRepository;
    private final ForumMembershipRepository membershipRepository;
    private final ForumService forumService;
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            URI uri = session.getUri();
            Map<String, String> query = parseQuery(uri != null ? uri.getQuery() : null);
            String token = query.get("token");
            String forumIdRaw = query.get("forumId");
            if (token == null || forumIdRaw == null) {
                log.warn("WS forum rechazado: faltan parametros");
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

            chatRealtimeBroadcaster.register(chatRealtimeBroadcaster.forumRoom(forumId), session);
            session.getAttributes().put("user", user);
            session.getAttributes().put("forumId", forumId);
            log.debug("WS forum conectado forum={} user={}", shortId(forumId), shortId(user.getId()));
        } catch (Exception e) {
            log.warn("WS forum rechazado: {}", e.getMessage());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID forumId = (UUID) session.getAttributes().get("forumId");
        User user = (User) session.getAttributes().get("user");
        if (forumId == null || user == null) {
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        String content = payload.get("content") != null ? payload.get("content").toString() : "";
        PostResponse saved = forumService.createPost(forumId, new CreatePostRequest(content), user);
        chatRealtimeBroadcaster.broadcast(chatRealtimeBroadcaster.forumRoom(forumId), saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRealtimeBroadcaster.unregister(session);
        log.debug("WS forum cerrado session={} status={}", session.getId(), status);
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
