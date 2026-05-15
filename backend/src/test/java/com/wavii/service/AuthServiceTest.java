package com.wavii.service;

import com.wavii.dto.auth.*;
import com.wavii.model.User;
import com.wavii.model.VerificationToken;
import com.wavii.model.enums.Role;
import com.wavii.repository.UserRepository;
import com.wavii.repository.VerificationTokenRepository;
import com.wavii.service.OdooService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private OdooService odooService;

    @InjectMocks private AuthService authService;

    private User user;
    private VerificationToken verificationToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setName("Test User");
        user.setPasswordHash("encoded_password");
        user.setRole(Role.USUARIO);
        user.setEmailVerified(true);

        verificationToken = new VerificationToken();
        verificationToken.setToken("token-123");
        verificationToken.setUser(user);
        verificationToken.setType("EMAIL_VERIFICATION");
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        verificationToken.setUsed(false);
    }

    @Test
    void registerSuccessTest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New User");
        request.setEmail("new@test.com");
        request.setPassword("password");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertEquals("new@test.com", response.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void registerEmailExistsTest() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@test.com");

        when(userRepository.existsByNameIgnoreCase("Test User")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginSuccessTest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void loginBadCredentialsTest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginEmailNotVerifiedTest() {
        user.setEmailVerified(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);

        AuthService.EmailNotVerifiedException exception = assertThrows(
                AuthService.EmailNotVerifiedException.class, () -> authService.login(request));
        assertEquals("EMAIL_NOT_VERIFIED", exception.getCode());
    }

    @Test
    void refreshTokenSuccessTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        when(jwtService.extractEmail("valid_refresh_token")).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid_refresh_token", user)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("new_access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new_refresh");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access", response.getAccessToken());
        assertEquals("new_refresh", response.getRefreshToken());
    }

    @Test
    void refreshTokenInvalidTest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        when(jwtService.extractEmail("invalid_token")).thenThrow(new IllegalArgumentException("invalid token"));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(request));
    }

    @Test
    void forgotPasswordTest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndTypeAndUsedFalse(user, "PASSWORD_RESET")).thenReturn(List.of());

        authService.forgotPassword(request);

        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resetPasswordSuccessTest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-123");
        request.setNewPassword("new_password");

        verificationToken.setType("PASSWORD_RESET");

        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(verificationToken));
        when(passwordEncoder.encode("new_password")).thenReturn("new_encoded_password");

        authService.resetPassword(request);

        verify(userRepository, times(1)).save(user);
        verify(tokenRepository, times(1)).save(verificationToken);
        assertEquals("new_encoded_password", user.getPasswordHash());
        assertTrue(verificationToken.isUsed());
    }

    @Test
    void verifyEmailSuccessTest() {
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(verificationToken));
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse response = authService.verifyEmail("token-123");

        assertNotNull(response);
        assertTrue(user.isEmailVerified());
        assertTrue(verificationToken.isUsed());
        verify(userRepository, times(1)).save(user);
        verify(tokenRepository, times(1)).save(verificationToken);
    }

    @Test
    void verifyEmailExpiredTokenTest() {
        verificationToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(verificationToken));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("token-123"));
    }

    @Test
    void resendVerificationSuccessTest() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndTypeAndUsedFalse(user, "EMAIL_VERIFICATION")).thenReturn(List.of());

        authService.resendVerification("test@test.com");

        verify(tokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerificationAlreadyVerifiedTest() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authService.resendVerification("test@test.com"));
    }
}
