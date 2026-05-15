package com.wavii.service;

import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.model.User;
import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setEmail("test@test.com");
    }

    @Test
    void completeOnboardingTest() {
        CompleteOnboardingRequest request = new CompleteOnboardingRequest();
        request.setRole(Role.USUARIO);
        request.setLevel(Level.PRINCIPIANTE);
        request.setAvatarUrl("http://example.com/avatar.png");

        when(userRepository.save(any(User.class))).thenReturn(currentUser);

        User result = onboardingService.completeOnboarding(currentUser, request);

        assertNotNull(result);
        assertEquals(Role.USUARIO, currentUser.getRole());
        assertEquals(Level.PRINCIPIANTE, currentUser.getLevel());
        assertEquals("http://example.com/avatar.png", currentUser.getAvatarUrl());
        assertTrue(currentUser.isOnboardingCompleted());
        verify(userRepository, times(1)).save(currentUser);
    }

    @Test
    void submitTeacherVerificationSuccessTest() {
        currentUser.setRole(Role.PROFESOR_PARTICULAR);
        assertDoesNotThrow(() -> onboardingService.submitTeacherVerification(currentUser));
    }

    @Test
    void submitTeacherVerificationFailureNotTeacherTest() {
        currentUser.setRole(Role.USUARIO);
        assertThrows(IllegalArgumentException.class, () -> onboardingService.submitTeacherVerification(currentUser));
    }

    @Test
    void getTeacherVerificationStatusVerifiedTest() {
        currentUser.setTeacherVerified(true);
        VerificationStatusResponse response = onboardingService.getTeacherVerificationStatus(currentUser);
        assertTrue(response.isTeacherVerified());
        assertEquals("Tu cuenta de profesor está verificada", response.getMessage());
    }

    @Test
    void getTeacherVerificationStatusPendingTest() {
        currentUser.setRole(Role.PROFESOR_PARTICULAR);
        currentUser.setTeacherVerified(false);
        VerificationStatusResponse response = onboardingService.getTeacherVerificationStatus(currentUser);
        assertFalse(response.isTeacherVerified());
        assertEquals("Tu solicitud de verificación está pendiente de revisión", response.getMessage());
    }

    @Test
    void getTeacherVerificationStatusNotActiveTest() {
        currentUser.setRole(Role.USUARIO);
        currentUser.setTeacherVerified(false);
        VerificationStatusResponse response = onboardingService.getTeacherVerificationStatus(currentUser);
        assertFalse(response.isTeacherVerified());
        assertEquals("No tienes una solicitud de verificación activa", response.getMessage());
    }
}
