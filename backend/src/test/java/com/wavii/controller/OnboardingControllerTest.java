package com.wavii.controller;

import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.model.User;
import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import com.wavii.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private OnboardingService onboardingService;

    @InjectMocks
    private OnboardingController onboardingController;

    @Test
    void completeOnboardingSuccessTest() {
        User user = new User();
        CompleteOnboardingRequest request = new CompleteOnboardingRequest();

        User updatedUser = new User();
        updatedUser.setOnboardingCompleted(true);
        updatedUser.setRole(Role.USUARIO);
        updatedUser.setLevel(Level.PRINCIPIANTE);

        when(onboardingService.completeOnboarding(user, request)).thenReturn(updatedUser);

        ResponseEntity<?> result = onboardingController.completeOnboarding(user, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void completeOnboardingErrorTest() {
        User user = new User();
        CompleteOnboardingRequest request = new CompleteOnboardingRequest();

        when(onboardingService.completeOnboarding(user, request)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = onboardingController.completeOnboarding(user, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void submitTeacherVerificationSuccessTest() {
        User user = new User();
        doNothing().when(onboardingService).submitTeacherVerification(user);

        ResponseEntity<?> result = onboardingController.submitTeacherVerification(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void submitTeacherVerificationBadRequestTest() {
        User user = new User();
        doThrow(new IllegalArgumentException("Error")).when(onboardingService).submitTeacherVerification(user);

        ResponseEntity<?> result = onboardingController.submitTeacherVerification(user);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
    }

    @Test
    void submitTeacherVerificationErrorTest() {
        User user = new User();
        doThrow(new RuntimeException("Error")).when(onboardingService).submitTeacherVerification(user);

        ResponseEntity<?> result = onboardingController.submitTeacherVerification(user);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void getVerificationStatusSuccessTest() {
        User user = new User();
        VerificationStatusResponse response = VerificationStatusResponse.builder().build();
        when(onboardingService.getTeacherVerificationStatus(user)).thenReturn(response);

        ResponseEntity<?> result = onboardingController.getVerificationStatus(user);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getVerificationStatusErrorTest() {
        User user = new User();
        when(onboardingService.getTeacherVerificationStatus(user)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> result = onboardingController.getVerificationStatus(user);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }

    @Test
    void completeOnboardingNullLevelReturnsOkWithNullLevelTest() {
        User user = new User();
        CompleteOnboardingRequest request = new CompleteOnboardingRequest();

        User updatedUser = mock(User.class);
        when(updatedUser.isOnboardingCompleted()).thenReturn(true);
        when(updatedUser.getRole()).thenReturn(Role.USUARIO);
        when(updatedUser.getLevel()).thenReturn(null);

        when(onboardingService.completeOnboarding(user, request)).thenReturn(updatedUser);

        ResponseEntity<?> result = onboardingController.completeOnboarding(user, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        java.util.Map<?, ?> body = (java.util.Map<?, ?>) result.getBody();
        org.junit.jupiter.api.Assertions.assertNull(body.get("level"));
    }
}
