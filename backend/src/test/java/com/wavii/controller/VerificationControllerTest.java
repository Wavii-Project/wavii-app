package com.wavii.controller;

import com.wavii.model.User;
import com.wavii.model.VerificationRequest;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.VerificationStatus;
import com.wavii.repository.UserRepository;
import com.wavii.repository.VerificationRequestRepository;
import com.wavii.service.EmailService;
import com.wavii.service.OdooService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OdooService odooService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationController verificationController;

    private User currentUser;
    private VerificationRequest verificationRequest;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setEmail("teacher@test.com");
        currentUser.setName("Prof. Test");
        currentUser.setRole(Role.PROFESOR_PARTICULAR);

        verificationRequest = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .user(currentUser)
                .fileName("certificate.pdf")
                .filePath("/app/uploads/verifications/cert.pdf")
                .status(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(verificationController, "odooWebhookSecret", "secret123");
        ReflectionTestUtils.setField(verificationController, "appBaseUrl", "http://localhost:8080");
    }

    // ── uploadDocument ────────────────────────────────────────────

    @Test
    void uploadDocumentEmptyFileReturnsBadRequestTest() {
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        ResponseEntity<?> result = verificationController.uploadDocument(emptyFile, currentUser);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("El archivo no puede estar vacío", body.get("message"));
    }

    // ── getStatus ─────────────────────────────────────────────────

    @Test
    void getStatusWithPendingRequestReturnsRequestInfoTest() {
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.of(verificationRequest));

        ResponseEntity<?> result = verificationController.getStatus(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("PENDING", body.get("status"));
        assertEquals("certificate.pdf", body.get("fileName"));
    }

    @Test
    void getStatusNoRequestReturnsNoneTest() {
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.empty());

        ResponseEntity<?> result = verificationController.getStatus(currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("NONE", body.get("status"));
    }

    // ── approveVerification ───────────────────────────────────────

    @Test
    void approveVerificationValidUserApprovesAndSendsEmailTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.of(verificationRequest));

        ResponseEntity<?> result = verificationController.approveVerification(currentUser.getId());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(currentUser.isTeacherVerified());
        assertEquals(Role.PROFESOR_CERTIFICADO, currentUser.getRole());
        assertEquals(VerificationStatus.APPROVED, verificationRequest.getStatus());
        verify(userRepository).save(currentUser);
        verify(emailService).sendVerificationApprovedEmail(currentUser.getEmail(), currentUser.getName());
    }

    @Test
    void approveVerificationValidUserNoRequestStillApprovesUserTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.empty());

        ResponseEntity<?> result = verificationController.approveVerification(currentUser.getId());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(currentUser.isTeacherVerified());
        verify(emailService).sendVerificationApprovedEmail(currentUser.getEmail(), currentUser.getName());
    }

    @Test
    void approveVerificationUserNotFoundThrowsExceptionTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> verificationController.approveVerification(unknownId));
    }

    // ── odooWebhook ───────────────────────────────────────────────

    @Test
    void odooWebhookInvalidSecretReturnsUnauthorizedTest() {
        ResponseEntity<?> result = verificationController.odooWebhook(
                "wrong_secret", Map.of("userId", currentUser.getId().toString(), "action", "approve"));

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void odooWebhookNullSecretReturnsUnauthorizedTest() {
        ResponseEntity<?> result = verificationController.odooWebhook(
                null, Map.of("userId", currentUser.getId().toString(), "action", "approve"));

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void odooWebhookMissingUserIdReturnsBadRequestTest() {
        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("action", "approve"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void odooWebhookMissingActionReturnsBadRequestTest() {
        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", currentUser.getId().toString()));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void odooWebhookInvalidUUIDReturnsBadRequestTest() {
        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", "not-a-uuid", "action", "approve"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void odooWebhookUserNotFoundReturnsNotFoundTest() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", unknownId.toString(), "action", "approve"));

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    void odooWebhookApproveActionApprovesUserTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.of(verificationRequest));

        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", currentUser.getId().toString(), "action", "approve"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(currentUser.isTeacherVerified());
        assertEquals(Role.PROFESOR_CERTIFICADO, currentUser.getRole());
        assertEquals(VerificationStatus.APPROVED, verificationRequest.getStatus());
        verify(emailService).sendVerificationApprovedEmail(currentUser.getEmail(), currentUser.getName());
    }

    @Test
    void odooWebhookApproveActionUpperCaseApprovesUserTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.empty());

        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", currentUser.getId().toString(), "action", "APPROVE"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(emailService).sendVerificationApprovedEmail(any(), any());
    }

    @Test
    void odooWebhookRejectActionRejectsRequestTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(verificationRequestRepository.findTopByUserOrderByCreatedAtDesc(currentUser))
                .thenReturn(Optional.of(verificationRequest));

        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", currentUser.getId().toString(), "action", "reject"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(VerificationStatus.REJECTED, verificationRequest.getStatus());
        verify(emailService).sendVerificationRejectedEmail(currentUser.getEmail(), currentUser.getName());
    }

    @Test
    void odooWebhookInvalidActionReturnsBadRequestTest() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

        ResponseEntity<?> result = verificationController.odooWebhook(
                "secret123", Map.of("userId", currentUser.getId().toString(), "action", "delete"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }
}
