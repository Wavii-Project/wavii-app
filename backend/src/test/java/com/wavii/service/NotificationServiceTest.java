package com.wavii.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wavii.model.AppNotification;
import com.wavii.model.User;
import com.wavii.repository.AppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AppNotificationRepository notificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(UUID.randomUUID());
        recipient.setName("Test User");
        recipient.setEmail("test@wavii.app");
    }

    @Test
    void createNotificationWithDataSavesAndReturnsTest() throws Exception {
        Map<String, Object> data = Map.of("key", "value");
        AppNotification saved = AppNotification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .type("test_type")
                .title("Test Title")
                .body("Test Body")
                .dataJson("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build();

        when(objectMapper.writeValueAsString(data)).thenReturn("{\"key\":\"value\"}");
        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);

        AppNotification result = notificationService.create(recipient, "test_type", "Test Title", "Test Body", data);

        assertNotNull(result);
        assertEquals("test_type", result.getType());
        assertEquals("Test Title", result.getTitle());
        verify(notificationRepository).save(any(AppNotification.class));
    }

    @Test
    void createNotificationWithNullDataSavesWithoutJsonTest() {
        AppNotification saved = AppNotification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .type("simple")
                .title("Title")
                .body("Body")
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);

        AppNotification result = notificationService.create(recipient, "simple", "Title", "Body", null);

        assertNotNull(result);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void createNotificationWithEmptyDataSavesWithoutJsonTest() {
        AppNotification saved = AppNotification.builder()
                .id(UUID.randomUUID())
                .recipient(recipient)
                .type("simple")
                .title("Title")
                .body("Body")
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(AppNotification.class))).thenReturn(saved);

        AppNotification result = notificationService.create(recipient, "simple", "Title", "Body", Map.of());

        assertNotNull(result);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void listReturnsNotificationsAsMapsTest() {
        UUID notifId = UUID.randomUUID();
        AppNotification notification = AppNotification.builder()
                .id(notifId)
                .recipient(recipient)
                .type("class_request")
                .title("Nueva solicitud")
                .body("Tienes una solicitud")
                .dataJson(null)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient))
                .thenReturn(List.of(notification));

        List<Map<String, Object>> result = notificationService.list(recipient);

        assertEquals(1, result.size());
        assertEquals(notifId.toString(), result.get(0).get("id"));
        assertEquals("class_request", result.get(0).get("type"));
        assertEquals("Nueva solicitud", result.get(0).get("title"));
        assertEquals(false, result.get(0).get("read"));
    }

    @Test
    void listReturnsEmptyListWhenNoNotificationsTest() {
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient))
                .thenReturn(List.of());

        List<Map<String, Object>> result = notificationService.list(recipient);

        assertTrue(result.isEmpty());
    }

    @Test
    void markReadSuccessReturnsUpdatedMapTest() {
        UUID notifId = UUID.randomUUID();
        AppNotification notification = AppNotification.builder()
                .id(notifId)
                .recipient(recipient)
                .type("info")
                .title("Info")
                .body("Cuerpo")
                .dataJson(null)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Map<String, Object> result = notificationService.markRead(recipient, notifId);

        assertNotNull(result);
        assertEquals(notifId.toString(), result.get("id"));
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markReadNotFoundThrowsExceptionTest() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markRead(recipient, notifId));
    }

    @Test
    void markReadWrongRecipientThrowsExceptionTest() {
        UUID notifId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        AppNotification notification = AppNotification.builder()
                .id(notifId)
                .recipient(otherUser)
                .type("info")
                .title("Info")
                .body("Body")
                .dataJson(null)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.markRead(recipient, notifId));
    }

    @Test
    void summaryReturnsUnreadCountTest() {
        when(notificationRepository.countByRecipientAndReadFalse(recipient)).thenReturn(5L);

        Map<String, Object> result = notificationService.summary(recipient);

        assertEquals(5L, result.get("unreadCount"));
    }

    @Test
    void clearAllReturnsRemovedCountTest() {
        when(notificationRepository.deleteByRecipient(recipient)).thenReturn(3L);

        Map<String, Object> result = notificationService.clearAll(recipient);

        assertEquals(3L, result.get("removed"));
        verify(notificationRepository).deleteByRecipient(recipient);
    }

    @Test
    void markAllReadReturnsUpdatedCountTest() {
        when(notificationRepository.markAllReadByRecipient(recipient)).thenReturn(7);

        Map<String, Object> result = notificationService.markAllRead(recipient);

        assertEquals(7, result.get("updated"));
        verify(notificationRepository).markAllReadByRecipient(recipient);
    }

    @Test
    void listWithDataJsonParsesCorrectlyTest() throws Exception {
        UUID notifId = UUID.randomUUID();
        AppNotification notification = AppNotification.builder()
                .id(notifId)
                .recipient(recipient)
                .type("class_request")
                .title("Solicitud")
                .body("Body")
                .dataJson("{\"enrollmentId\":\"abc\"}")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient))
                .thenReturn(List.of(notification));
        when(objectMapper.readValue(eq("{\"enrollmentId\":\"abc\"}"), eq(Map.class)))
                .thenReturn(Map.of("enrollmentId", "abc"));

        List<Map<String, Object>> result = notificationService.list(recipient);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).get("data"));
    }

    @Test
    void listWithInvalidDataJsonReturnsEmptyMapTest() throws Exception {
        UUID notifId = UUID.randomUUID();
        AppNotification notification = AppNotification.builder()
                .id(notifId)
                .recipient(recipient)
                .type("info")
                .title("Info")
                .body("Body")
                .dataJson("{invalid}")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient))
                .thenReturn(List.of(notification));
        when(objectMapper.readValue(eq("{invalid}"), eq(Map.class)))
                .thenThrow(new JsonProcessingException("error") {});

        List<Map<String, Object>> result = notificationService.list(recipient);

        assertEquals(1, result.size());
        assertEquals(Map.of(), result.get(0).get("data"));
    }

    @Test
    void createNotificationJsonProcessingExceptionResultsInNullDataJsonTest() throws Exception {
        Map<String, Object> data = Map.of("key", "value");
        when(objectMapper.writeValueAsString(data)).thenThrow(new JsonProcessingException("error") {});
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(invocation -> {
            AppNotification n = invocation.getArgument(0);
            assertNull(n.getDataJson());
            n.setId(UUID.randomUUID());
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        AppNotification result = notificationService.create(recipient, "type", "title", "body", data);

        assertNotNull(result);
    }
}
