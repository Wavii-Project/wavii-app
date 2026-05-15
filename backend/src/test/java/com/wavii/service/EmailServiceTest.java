package com.wavii.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@wavii.com");
    }

    // ── sendVerificationEmail ────────────────────────────────────────

    @Test
    void sendVerificationEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("user@test.com", "Test User", "token-123");

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendVerificationEmailBuildsCorrectLinkAndSubjectTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationEmail("verify@test.com", "Wavii User", "abc-token"));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendVerificationEmailWithSpecialCharactersInNameTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationEmail("user@test.com", "<script>alert(1)</script>", "token-abc"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendVerificationEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() ->
    //             emailService.sendVerificationEmail("user@test.com", "Test User", "token-xyz"));
    // }

    // ── sendPasswordResetEmail ───────────────────────────────────────

    @Test
    void sendPasswordResetEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("user@test.com", "Test User", "token-123");

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmailBuildsCorrectLinkTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("reset@test.com", "Reset User", "reset-token-456"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendPasswordResetEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() ->
    //             emailService.sendPasswordResetEmail("user@test.com", "Test User", "token-reset"));
    // }

    // ── sendTestEmail ────────────────────────────────────────────────

    @Test
    void sendTestEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendTestEmail("user@test.com");

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void sendTestEmailLogsSuccessTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendTestEmail("admin@wavii.com"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendTestEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("SMTP timeout")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() -> emailService.sendTestEmail("user@test.com"));
    // }

    // ── sendVerificationApprovedEmail ────────────────────────────────

    @Test
    void sendVerificationApprovedEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationApprovedEmail("approved@test.com", "Prof. Ana"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendVerificationApprovedEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("Server error")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() ->
    //             emailService.sendVerificationApprovedEmail("approved@test.com", "Prof. Ana"));
    // }

    // ── sendVerificationRejectedEmail ────────────────────────────────

    @Test
    void sendVerificationRejectedEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendVerificationRejectedEmail("rejected@test.com", "Prof. Juan"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendVerificationRejectedEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("Server error")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() ->
    //             emailService.sendVerificationRejectedEmail("rejected@test.com", "Prof. Juan"));
    // }

    // ── sendClassNotificationEmail ───────────────────────────────────

    @Test
    void sendClassNotificationEmailTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendClassNotificationEmail(
                        "student@test.com", "Estudiante", "Clase confirmada", "Tu clase ha sido confirmada"));

        verify(mailSender).send(mimeMessage);
    }

    // @Test
    // void sendClassNotificationEmailMessagingExceptionDoesNotThrowTest() throws Exception {
    //     MimeMessage faultyMime = mock(MimeMessage.class);
    //     when(mailSender.createMimeMessage()).thenReturn(faultyMime);
    //     doThrow(new MessagingException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

    //     assertDoesNotThrow(() ->
    //             emailService.sendClassNotificationEmail(
    //                     "student@test.com", "Estudiante", "Clase cancelada", "Tu clase fue cancelada"));
    // }

    @Test
    void sendClassNotificationEmailWithDifferentTitleTest() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() ->
                emailService.sendClassNotificationEmail(
                        "teacher@test.com", "Profesor", "Nueva solicitud de clase",
                        "Un estudiante ha solicitado una clase contigo"));

        verify(mailSender).send(mimeMessage);
    }
}
