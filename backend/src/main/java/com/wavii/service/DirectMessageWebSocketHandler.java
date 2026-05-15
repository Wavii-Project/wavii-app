package com.wavii.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.dto.dm.DirectMessageDto;
import com.wavii.model.User;
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
public class DirectMessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final DirectMessageService directMessageService;
    private final ChatRealtimeBroadcaster chatRealtimeBroadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            URI uri = session.getUri();
            Map<String, String> query = parseQuery(uri != null ? uri.getQuery() : null);
            String token = query.get("token");
            String otherIdRaw = query.get("userId");
            if (token == null || otherIdRaw == null) {
                log.warn("WS direct rechazado: faltan parametros");
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            User me = authenticate(token);
            UUID otherId = UUID.fromString(otherIdRaw);
            if (me.getId().equals(otherId)) {
                log.warn("WS direct rechazado: usuario intento abrir chat consigo mismo {}", shortId(me.getId()));
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            User other = userRepository.findById(otherId).orElse(null);
            if (other == null) {
                log.warn("WS direct rechazado: destinatario no encontrado {}", shortId(otherId));
                session.close(CloseStatus.BAD_DATA);
                return;
            }

            String roomId = chatRealtimeBroadcaster.directRoom(me.getId(), otherId);
            chatRealtimeBroadcaster.register(roomId, session);
            session.getAttributes().put("me", me);
            session.getAttributes().put("otherId", otherId);
            log.debug("WS direct conectado room={} user={}", roomId, shortId(me.getId()));
        } catch (Exception e) {
            log.warn("WS direct rechazado: {}", e.getMessage());
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User me = (User) session.getAttributes().get("me");
        UUID otherId = (UUID) session.getAttributes().get("otherId");
        if (me == null || otherId == null) {
            return;
        }

        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        DirectMessageDto saved = directMessageService.sendMessage(me, otherId, payload.get("content") != null ? payload.get("content").toString() : null);
        chatRealtimeBroadcaster.broadcast(chatRealtimeBroadcaster.directRoom(me.getId(), otherId), saved);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        chatRealtimeBroadcaster.unregister(session);
        log.debug("WS direct cerrado session={} status={}", session.getId(), status);
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
