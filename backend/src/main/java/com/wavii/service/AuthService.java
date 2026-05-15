package com.wavii.service;

import com.wavii.dto.auth.*;
import com.wavii.model.User;
import com.wavii.model.VerificationToken;
import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import com.wavii.repository.UserRepository;
import com.wavii.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String TYPE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final OdooService odooService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByNameIgnoreCase(request.getName().strip())) {
            throw new IllegalArgumentException("__NAME__Este nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USUARIO)
                .subscription(Subscription.FREE)
                .emailVerified(false)
                .onboardingCompleted(false)
                .teacherVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.debug("Usuario registrado: {}", user.getEmail());

        String verificationToken = createVerificationToken(user, TYPE_EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("EMAIL_NOT_VERIFIED",
                    "Debes verificar tu email antes de iniciar sesión");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.debug("Login exitoso: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!jwtService.isTokenValid(token, user)) {
            throw new IllegalArgumentException("Refresh token inválido o expirado");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            invalidatePreviousTokens(user, TYPE_PASSWORD_RESET);
            String resetToken = createVerificationToken(user, TYPE_PASSWORD_RESET);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken verificationToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (verificationToken.isUsed()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace ha expirado");
        }
        if (!TYPE_PASSWORD_RESET.equals(verificationToken.getType())) {
            throw new IllegalArgumentException("Token de tipo incorrecto");
        }

        User user = verificationToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.debug("Contraseña restablecida para: {}", user.getEmail());
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        if (verificationToken.isUsed()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace ha expirado");
        }
        if (!TYPE_EMAIL_VERIFICATION.equals(verificationToken.getType())) {
            throw new IllegalArgumentException("Token de tipo incorrecto");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.debug("Email verificado para: {}", user.getEmail());

        odooService.createCrmContact(
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getSubscription().name()
        );

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta con ese email"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("El email ya está verificado");
        }

        invalidatePreviousTokens(user, TYPE_EMAIL_VERIFICATION);
        String newToken = createVerificationToken(user, TYPE_EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), newToken);
    }

    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::isEmailVerified)
                .orElse(false);
    }

    // ---- Private helpers ----

    private String createVerificationToken(User user, String type) {
        VerificationToken vt = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(vt);
        return vt.getToken();
    }

    private void invalidatePreviousTokens(User user, String type) {
        List<VerificationToken> existing = tokenRepository.findAllByUserAndTypeAndUsedFalse(user, type);
        existing.forEach(t -> t.setUsed(true));
        tokenRepository.saveAll(existing);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .city(user.getCity())
                .role(user.getRole())
                .subscription(user.getSubscription())
                .emailVerified(user.isEmailVerified())
                .onboardingCompleted(user.isOnboardingCompleted())
                .teacherVerified(user.isTeacherVerified())
                .build();
    }

    // ---- Custom exception ----

    public static class EmailNotVerifiedException extends RuntimeException {
        private final String code;

        public EmailNotVerifiedException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
