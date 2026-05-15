package com.wavii.dto.onboarding;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificationStatusResponse {

    private boolean teacherVerified;
    private boolean onboardingCompleted;
    private String message;
}
