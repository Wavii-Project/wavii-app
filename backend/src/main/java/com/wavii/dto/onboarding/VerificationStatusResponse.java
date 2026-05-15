package com.wavii.dto.onboarding;

import lombok.Builder;
import lombok.Data;

/**
 * DTO que informa sobre el estado de verificación y onboarding del usuario.
 */
@Data
@Builder
public class VerificationStatusResponse {

    private boolean teacherVerified;
    private boolean onboardingCompleted;
    private String message;
}
