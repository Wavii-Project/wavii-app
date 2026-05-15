package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setName("Test User");
        currentUser.setEmail("test@wavii.app");
    }

    @Test
    void listNullUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = notificationController.list(null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void listValidUserReturnsOkWithItemsAndSummaryTest() {
        when(notificationService.list(currentUser)).thenReturn(List.of());
        when(notificationService.summary(currentUser)).thenReturn(Map.of("unreadCount", 0L));

        ResponseEntity<?> result = notificationController.list(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("items"));
        assertTrue(body.containsKey("summary"));
        verify(notificationService).list(currentUser);
        verify(notificationService).summary(currentUser);
    }

    @Test
    void markReadNullUserReturnsUnauthorizedTest() {
        UUID notificationId = UUID.randomUUID();

        ResponseEntity<?> result = notificationController.markRead(null, notificationId);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void markReadSuccessReturnsOkTest() {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(currentUser, notificationId))
                .thenReturn(Map.of("id", notificationId.toString(), "read", true));

        ResponseEntity<?> result = notificationController.markRead(currentUser, notificationId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(notificationService).markRead(currentUser, notificationId);
    }

    @Test
    void markReadServiceThrowsReturnsBadRequestTest() {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(currentUser, notificationId))
                .thenThrow(new IllegalArgumentException("Notificacion no encontrada"));

        ResponseEntity<?> result = notificationController.markRead(currentUser, notificationId);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals("Notificacion no encontrada", body.get("message"));
    }

    @Test
    void markAllReadNullUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = notificationController.markAllRead(null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void markAllReadSuccessReturnsOkTest() {
        when(notificationService.markAllRead(currentUser)).thenReturn(Map.of("updated", 3));

        ResponseEntity<?> result = notificationController.markAllRead(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals(3, body.get("updated"));
        verify(notificationService).markAllRead(currentUser);
    }

    @Test
    void clearAllNullUserReturnsUnauthorizedTest() {
        ResponseEntity<?> result = notificationController.clearAll(null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        verifyNoInteractions(notificationService);
    }

    @Test
    void clearAllSuccessReturnsOkTest() {
        when(notificationService.clearAll(currentUser)).thenReturn(Map.of("removed", 5L));

        ResponseEntity<?> result = notificationController.clearAll(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertNotNull(body);
        assertEquals(5L, body.get("removed"));
        verify(notificationService).clearAll(currentUser);
    }
}
