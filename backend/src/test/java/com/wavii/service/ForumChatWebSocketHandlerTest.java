package com.wavii.service;

import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.PostResponse;
import com.wavii.model.Forum;
import com.wavii.model.User;
import com.wavii.repository.ForumMembershipRepository;
import com.wavii.repository.ForumRepository;
import com.wavii.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForumChatWebSocketHandlerTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private ForumRepository forumRepository;
    @Mock private ForumMembershipRepository membershipRepository;
    @Mock private ForumService forumService;

    @InjectMocks private ForumChatWebSocketHandler handler;

    private WebSocketSession session;
    private User user;
    private Forum forum;
    private UUID forumId;

    @BeforeEach
    void setUp() {
        session = mock(WebSocketSession.class);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");

        forumId = UUID.randomUUID();
        forum = new Forum();
        forum.setId(forumId);

        lenient().when(session.getId()).thenReturn("session-1");
        lenient().when(session.getAttributes()).thenReturn(new HashMap<>());
    }

    // ─── afterConnectionEstablished ────────────────────────────────

    @Test
    void afterConnectionEstablishedNullUriClosesSessionTest() throws Exception {
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablishedMissingTokenClosesSessionTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablishedMissingForumIdClosesSessionTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=someToken");
        when(session.getUri()).thenReturn(uri);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablishedSuccessTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
        assertEquals(user, session.getAttributes().get("user"));
        assertEquals(forumId, session.getAttributes().get("forumId"));
    }

    @Test
    void afterConnectionEstablishedBearerPrefixInTokenTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=Bearer%20rawToken&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("rawToken")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("rawToken", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedForumNotFoundThrowsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedNotMemberThrowsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedInvalidTokenThrowsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=invalid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("invalid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid", user)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    // ─── handleTextMessage ────────────────────────────────────────

    @Test
    void handleTextMessageNullAttributesReturnsEarlyTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        when(session.getAttributes()).thenReturn(attrs);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verifyNoInteractions(forumService);
    }

    @Test
    void handleTextMessageNullForumIdReturnsEarlyTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("user", user);
        when(session.getAttributes()).thenReturn(attrs);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verifyNoInteractions(forumService);
    }

    @Test
    void handleTextMessageSuccessTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("forumId", forumId);
        attrs.put("user", user);
        when(session.getAttributes()).thenReturn(attrs);

        PostResponse postResponse = new PostResponse(
                UUID.randomUUID().toString(), "hello",
                user.getId().toString(), "User", null, "2024-01-01T00:00:00");
        when(forumService.createPost(eq(forumId), any(CreatePostRequest.class), eq(user)))
                .thenReturn(postResponse);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verify(forumService).createPost(eq(forumId), any(CreatePostRequest.class), eq(user));
    }

    @Test
    void handleTextMessageNullContentInPayloadTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("forumId", forumId);
        attrs.put("user", user);
        when(session.getAttributes()).thenReturn(attrs);

        PostResponse postResponse = new PostResponse(
                UUID.randomUUID().toString(), "",
                user.getId().toString(), "User", null, "2024-01-01T00:00:00");
        when(forumService.createPost(eq(forumId), any(CreatePostRequest.class), eq(user)))
                .thenReturn(postResponse);

        TextMessage msg = new TextMessage("{\"other\":\"field\"}");
        handler.handleTextMessage(session, msg);

        verify(forumService).createPost(eq(forumId), any(CreatePostRequest.class), eq(user));
    }

    // ─── afterConnectionClosed ────────────────────────────────────

    @Test
    void afterConnectionClosedNoSessionInMapReturnsEarlyTest() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }

    @Test
    void afterConnectionClosedRemovesSessionFromRoomTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }

    // ─── broadcast via handleTextMessage ─────────────────────────

    @Test
    void handleTextMessageBroadcastsToOpenSessionsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(session2.isOpen()).thenReturn(true);

        when(session.getUri()).thenReturn(uri);
        when(session2.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);

        PostResponse postResponse = new PostResponse(
                UUID.randomUUID().toString(), "hello",
                user.getId().toString(), "User", null, "2024-01-01T00:00:00");
        when(forumService.createPost(eq(forumId), any(com.wavii.dto.forum.CreatePostRequest.class), eq(user)))
                .thenReturn(postResponse);

        Map<String, Object> attrs = session.getAttributes();
        attrs.put("forumId", forumId);
        attrs.put("user", user);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosedMultipleSessionsKeepsRoomTest() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(new HashMap<>());
        URI uri = new URI("ws://localhost/ws/forum?token=valid&forumId=" + forumId);
        when(session.getUri()).thenReturn(uri);
        when(session2.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid", user)).thenReturn(true);
        when(forumRepository.findById(forumId)).thenReturn(Optional.of(forum));
        when(membershipRepository.existsByForumAndUser(forum, user)).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }
}
