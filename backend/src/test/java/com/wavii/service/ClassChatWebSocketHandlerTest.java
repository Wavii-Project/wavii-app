package com.wavii.service;

import com.wavii.model.ClassEnrollment;
import com.wavii.model.User;
import com.wavii.repository.ClassEnrollmentRepository;
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
class ClassChatWebSocketHandlerTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassService classService;

    @InjectMocks private ClassChatWebSocketHandler handler;

    private WebSocketSession session;
    private User teacher;
    private User student;
    private ClassEnrollment enrollment;
    private UUID enrollmentId;

    @BeforeEach
    void setUp() {
        session = mock(WebSocketSession.class);

        teacher = new User();
        teacher.setId(UUID.randomUUID());
        teacher.setEmail("teacher@test.com");

        student = new User();
        student.setId(UUID.randomUUID());
        student.setEmail("student@test.com");

        enrollmentId = UUID.randomUUID();
        enrollment = new ClassEnrollment();
        enrollment.setId(enrollmentId);
        enrollment.setTeacher(teacher);
        enrollment.setStudent(student);
        enrollment.setPaymentStatus("accepted");

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
        URI uri = new URI("ws://localhost/ws/class?enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablishedMissingEnrollmentIdClosesSessionTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid");
        when(session.getUri()).thenReturn(uri);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void afterConnectionEstablishedTeacherSuccessTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
        assertEquals(teacher, session.getAttributes().get("user"));
    }

    @Test
    void afterConnectionEstablishedStudentSuccessTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("student@test.com");
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(jwtService.isTokenValid("valid", student)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
        assertEquals(student, session.getAttributes().get("user"));
    }

    @Test
    void afterConnectionEstablishedPaidStatusSuccessTest() throws Exception {
        enrollment.setPaymentStatus("paid");
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedScheduledStatusSuccessTest() throws Exception {
        enrollment.setPaymentStatus("scheduled");
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedCompletedStatusSuccessTest() throws Exception {
        enrollment.setPaymentStatus("completed");
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedRefundRequestedStatusSuccessTest() throws Exception {
        enrollment.setPaymentStatus("refund_requested");
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedNullPaymentStatusThrowsTest() throws Exception {
        enrollment.setPaymentStatus(null);
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedPendingStatusThrowsTest() throws Exception {
        enrollment.setPaymentStatus("pending");
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedEnrollmentNotFoundThrowsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedUnauthorizedUserThrowsTest() throws Exception {
        User other = new User();
        other.setId(UUID.randomUUID());
        other.setEmail("other@test.com");

        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("other@test.com");
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));
        when(jwtService.isTokenValid("valid", other)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        assertThrows(IllegalArgumentException.class,
                () -> handler.afterConnectionEstablished(session));
    }

    @Test
    void afterConnectionEstablishedBearerTokenPrefixTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=Bearer%20rawToken&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("rawToken")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("rawToken", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablishedInvalidTokenThrowsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=invalid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("invalid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("invalid", teacher)).thenReturn(false);

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

        verifyNoInteractions(classService);
    }

    @Test
    void handleTextMessageNullEnrollmentIdReturnsEarlyTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("user", teacher);
        when(session.getAttributes()).thenReturn(attrs);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verifyNoInteractions(classService);
    }

    @Test
    void handleTextMessageSuccessTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("enrollmentId", enrollmentId);
        attrs.put("user", teacher);
        when(session.getAttributes()).thenReturn(attrs);

        Map<String, Object> savedMsg = Map.of("content", "hello");
        when(classService.sendMessage(eq(enrollmentId), eq(teacher), anyString()))
                .thenReturn(savedMsg);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verify(classService).sendMessage(eq(enrollmentId), eq(teacher), eq("hello"));
    }

    @Test
    void handleTextMessageNullContentInPayloadTest() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("enrollmentId", enrollmentId);
        attrs.put("user", teacher);
        when(session.getAttributes()).thenReturn(attrs);

        Map<String, Object> savedMsg = Map.of("content", "");
        when(classService.sendMessage(eq(enrollmentId), eq(teacher), eq("")))
                .thenReturn(savedMsg);

        TextMessage msg = new TextMessage("{\"other\":\"field\"}");
        handler.handleTextMessage(session, msg);

        verify(classService).sendMessage(eq(enrollmentId), eq(teacher), eq(""));
    }

    // ─── afterConnectionClosed ────────────────────────────────────

    @Test
    void afterConnectionClosedNoSessionInMapReturnsEarlyTest() {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }

    @Test
    void afterConnectionClosedRemovesSessionTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }

    // ─── broadcast via handleTextMessage ─────────────────────────

    @Test
    void handleTextMessageBroadcastsToOpenSessionsTest() throws Exception {
        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(new java.util.HashMap<>());
        when(session2.isOpen()).thenReturn(true);

        when(session.getUri()).thenReturn(uri);
        when(session2.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);

        // Manually add session2 to the room by connecting it as well
        handler.afterConnectionEstablished(session2);

        Map<String, Object> savedMsg = Map.of("content", "hello");
        when(classService.sendMessage(eq(enrollmentId), eq(teacher), eq("hello")))
                .thenReturn(savedMsg);

        Map<String, Object> attrs = session.getAttributes();
        attrs.put("enrollmentId", enrollmentId);
        attrs.put("user", teacher);

        TextMessage msg = new TextMessage("{\"content\":\"hello\"}");
        handler.handleTextMessage(session, msg);

        verify(session2).sendMessage(any(TextMessage.class));
    }

    @Test
    void afterConnectionClosedMultipleSessionsKeepsRoomTest() throws Exception {
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session2.getId()).thenReturn("session-2");
        when(session2.getAttributes()).thenReturn(new HashMap<>());

        URI uri = new URI("ws://localhost/ws/class?token=valid&enrollmentId=" + enrollmentId);
        when(session.getUri()).thenReturn(uri);
        when(session2.getUri()).thenReturn(uri);
        when(jwtService.extractEmail("valid")).thenReturn("teacher@test.com");
        when(userRepository.findByEmail("teacher@test.com")).thenReturn(Optional.of(teacher));
        when(jwtService.isTokenValid("valid", teacher)).thenReturn(true);
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(session2);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, atLeastOnce()).getId();
    }
}
