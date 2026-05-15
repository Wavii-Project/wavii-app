package com.wavii.controller;

import com.wavii.dto.auth.*;
import com.wavii.model.User;
import com.wavii.repository.UserRepository;
import com.wavii.service.AuthService;
import com.wavii.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerSuccessTest() {
        RegisterRequest request = new RegisterRequest();
        AuthResponse response = AuthResponse.builder().build();
        when(authService.register(request)).thenReturn(response);

        ResponseEntity<?> result = authController.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void registerConflictTest() {
        RegisterRequest request = new RegisterRequest();
        when(authService.register(request)).thenThrow(new IllegalArgumentException("Error"));

        ResponseEntity<?> result = authController.register(request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void loginSuccessTest() {
        LoginRequest request = new LoginRequest();
        AuthResponse response = AuthResponse.builder().build();
        when(authService.login(request)).thenReturn(response);

        ResponseEntity<?> result = authController.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void loginEmailNotVerifiedTest() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new AuthService.EmailNotVerifiedException("EMAIL_NOT_VERIFIED", "Error"));

        ResponseEntity<?> result = authController.login(request);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void loginBadCredentialsTest() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new BadCredentialsException("Error"));

        ResponseEntity<?> result = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void refreshSuccessTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        AuthResponse response = AuthResponse.builder().build();
        when(authService.refreshToken(request)).thenReturn(response);

        ResponseEntity<?> result = authController.refresh(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void refreshUnauthorizedTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        when(authService.refreshToken(request)).thenThrow(new IllegalArgumentException("Error"));

        ResponseEntity<?> result = authController.refresh(request);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void logoutTest() {
        ResponseEntity<?> result = authController.logout();
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void forgotPasswordSuccessTest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        doNothing().when(authService).forgotPassword(request);

        ResponseEntity<?> result = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void resetPasswordSuccessTest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        doNothing().when(authService).resetPassword(request);

        ResponseEntity<?> result = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void resetPasswordBadRequestTest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        doThrow(new IllegalArgumentException("Error")).when(authService).resetPassword(request);

        ResponseEntity<?> result = authController.resetPassword(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void verifyEmailSuccessTest() {
        AuthResponse response = AuthResponse.builder().build();
        when(authService.verifyEmail("token-123")).thenReturn(response);

        ResponseEntity<?> result = authController.verifyEmail("token-123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void verifyEmailBadRequestTest() {
        when(authService.verifyEmail("token-123")).thenThrow(new IllegalArgumentException("Error"));

        ResponseEntity<?> result = authController.verifyEmail("token-123");

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void resendVerificationSuccessTest() {
        doNothing().when(authService).resendVerification("test@test.com");

        ResponseEntity<?> result = authController.resendVerification(Map.of("email", "test@test.com"));

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void resendVerificationNoEmailTest() {
        ResponseEntity<?> result = authController.resendVerification(Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void resendVerificationBadRequestTest() {
        doThrow(new IllegalArgumentException("Error")).when(authService).resendVerification("test@test.com");

        ResponseEntity<?> result = authController.resendVerification(Map.of("email", "test@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void testEmailSuccessTest() {
        doNothing().when(emailService).sendTestEmail("test@test.com");

        ResponseEntity<?> result = authController.testEmail("test@test.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void registerExceptionTest() {
        RegisterRequest request = new RegisterRequest();
        when(authService.register(request)).thenThrow(new RuntimeException("Error genérico"));
        ResponseEntity<?> result = authController.register(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void loginExceptionTest() {
        LoginRequest request = new LoginRequest();
        when(authService.login(request)).thenThrow(new RuntimeException("Error genérico"));
        ResponseEntity<?> result = authController.login(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void refreshExceptionTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        when(authService.refreshToken(request)).thenThrow(new RuntimeException("Error genérico"));
        ResponseEntity<?> result = authController.refresh(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void forgotPasswordExceptionTest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        doThrow(new RuntimeException("Error genérico")).when(authService).forgotPassword(request);
        ResponseEntity<?> result = authController.forgotPassword(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void resetPasswordExceptionTest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        doThrow(new RuntimeException("Error genérico")).when(authService).resetPassword(request);
        ResponseEntity<?> result = authController.resetPassword(request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void verifyEmailExceptionTest() {
        when(authService.verifyEmail("token-123")).thenThrow(new RuntimeException("Error genérico"));
        ResponseEntity<?> result = authController.verifyEmail("token-123");
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void resendVerificationExceptionTest() {
        doThrow(new RuntimeException("Error genérico")).when(authService).resendVerification("test@test.com");
        ResponseEntity<?> result = authController.resendVerification(Map.of("email", "test@test.com"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void testEmailExceptionTest() {
        doThrow(new RuntimeException("Error genérico")).when(emailService).sendTestEmail("test@test.com");
        ResponseEntity<?> result = authController.testEmail("test@test.com");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    // ─── checkName ────────────────────────────────────────────────

    @Test
    void checkNameNotTakenReturnsFalseTest() {
        when(userRepository.existsByNameIgnoreCase("NewName")).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> result = authController.checkName("NewName", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(false, result.getBody().get("taken"));
    }

    @Test
    void checkNameTakenNoCurrentUserReturnsTrueTest() {
        when(userRepository.existsByNameIgnoreCase("TakenName")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> result = authController.checkName("TakenName", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().get("taken"));
    }

    @Test
    void checkNameCurrentUserSameNameReturnsFalseTest() {
        User currentUser = new User();
        currentUser.setName("MyName");
        when(userRepository.existsByNameIgnoreCase("MyName")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> result = authController.checkName("MyName", currentUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(false, result.getBody().get("taken"));
    }

    // ─── checkVerification ────────────────────────────────────────

    @Test
    void checkVerificationVerifiedReturnsTrueTest() {
        when(authService.isEmailVerified("test@test.com")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> result = authController.checkVerification("test@test.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().get("verified"));
    }

    @Test
    void checkVerificationNotVerifiedReturnsFalseTest() {
        when(authService.isEmailVerified("test@test.com")).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> result = authController.checkVerification("test@test.com");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(false, result.getBody().get("verified"));
    }

    // ─── resetPasswordForm ────────────────────────────────────────

    @Test
    void resetPasswordFormReturnsHtmlTest() {
        ResponseEntity<String> result = authController.resetPasswordForm("my-token");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().contains("my-token"));
    }
}
