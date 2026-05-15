package com.wavii.service;

import com.wavii.dto.onboarding.CompleteOnboardingRequest;
import com.wavii.dto.onboarding.VerificationStatusResponse;
import com.wavii.model.User;
import com.wavii.model.enums.Role;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;

    /**
     * Completa el proceso de onboarding del usuario, actualizando su rol, nivel y avatar.
     * 
     * @param currentUser Usuario actual.
     * @param request Datos del onboarding completado.
     * @return El usuario actualizado.
     */
    @Transactional
    public User completeOnboarding(User currentUser, CompleteOnboardingRequest request) {
        currentUser.setRole(request.getRole());
        currentUser.setLevel(request.getLevel());
        currentUser.setOnboardingCompleted(true);

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            currentUser.setAvatarUrl(request.getAvatarUrl());
        }

        User saved = userRepository.save(currentUser);
        log.debug("Onboarding completado para: {}", saved.getEmail());
        return saved;
    }

    /**
     * Registra una solicitud de verificación para un profesor.
     * 
     * @param currentUser Usuario profesor solicitante.
     */
    @Transactional
    public void submitTeacherVerification(User currentUser) {
        if (currentUser.getRole() != Role.PROFESOR_PARTICULAR
                && currentUser.getRole() != Role.PROFESOR_CERTIFICADO) {
            throw new IllegalArgumentException("Solo los profesores pueden solicitar verificación");
        }
        log.info("[TEACHER VERIFICATION] Solicitud de verificación recibida de: {} ({})",
                currentUser.getEmail(), currentUser.getRole());
    }

    /**
     * Obtiene el estado actual de la verificación de profesor para el usuario.
     * 
     * @param currentUser Usuario actual.
     * @return Respuesta con el estado y mensaje explicativo.
     */
    public VerificationStatusResponse getTeacherVerificationStatus(User currentUser) {
        String message;
        if (currentUser.isTeacherVerified()) {
            message = "Tu cuenta de profesor está verificada";
        } else if (currentUser.getRole() == Role.PROFESOR_PARTICULAR
                || currentUser.getRole() == Role.PROFESOR_CERTIFICADO) {
            message = "Tu solicitud de verificación está pendiente de revisión";
        } else {
            message = "No tienes una solicitud de verificación activa";
        }

        return VerificationStatusResponse.builder()
                .teacherVerified(currentUser.isTeacherVerified())
                .onboardingCompleted(currentUser.isOnboardingCompleted())
                .message(message)
                .build();
    }
}
