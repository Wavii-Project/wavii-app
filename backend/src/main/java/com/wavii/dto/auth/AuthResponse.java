package com.wavii.dto.auth;

import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String name;
    private String email;
    private String city;
    private Role role;
    private Subscription subscription;
    private boolean emailVerified;
    private boolean onboardingCompleted;
    private boolean teacherVerified;
}
