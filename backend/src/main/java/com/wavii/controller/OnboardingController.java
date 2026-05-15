package com.wavii.controller;

import com.wavii.dto.auth.AuthResponse;
import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.model.User;
import com.wavii.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Finaliza el proceso de onboarding para el usuario actual.
     * 
     * @param currentUser Usuario autenticado.
     * @param request Datos del onboarding (intereses, instrumentos, nivel).
     * @return 200 OK con el estado actualizado del usuario.
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeOnboarding(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        try {
            User updated = onboardingService.completeOnboarding(currentUser, request);
            Map<String, Object> body = new HashMap<>();
            body.put("message", "Onboarding completado");
            body.put("onboardingCompleted", updated.isOnboardingCompleted());
            body.put("role", updated.getRole().name());
            body.put("level", updated.getLevel() != null ? updated.getLevel().name() : null);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Error completando onboarding", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SERVER_ERROR", "message", "Error interno del servidor"));
        }
    }

    /**
     * Envía una solicitud de verificación para el rol de profesor certificado.
     * 
     * @param currentUser Usuario autenticado.
     * @return 200 OK si se envió la solicitud.
     */
    @PostMapping("/teacher/submit-verification")
    public ResponseEntity<?> submitTeacherVerification(
            @AuthenticationPrincipal User currentUser
    ) {
        try {
            onboardingService.submitTeacherVerification(currentUser);
            return ResponseEntity.ok(Map.of(
                    "message", "Solicitud de verificación enviada correctamente"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "BAD_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en submit-verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SERVER_ERROR", "message", "Error interno del servidor"));
        }
    }

    /**
     * Obtiene el estado actual de la solicitud de verificación del profesor.
     * 
     * @param currentUser Usuario autenticado.
     * @return 200 OK con el estado de verificación.
     */
    @GetMapping("/teacher/verification-status")
    public ResponseEntity<?> getVerificationStatus(
            @AuthenticationPrincipal User currentUser
    ) {
        try {
            VerificationStatusResponse status = onboardingService.getTeacherVerificationStatus(currentUser);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error obteniendo estado de verificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SERVER_ERROR", "message", "Error interno del servidor"));
        }
    }
}
