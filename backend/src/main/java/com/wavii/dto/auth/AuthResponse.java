package com.wavii.dto.auth;

import com.wavii.model.enums.Role;
import com.wavii.model.enums.Subscription;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * DTO que contiene la respuesta tras una autenticación exitosa.
 * Incluye los tokens JWT y la información básica del perfil del usuario.
 * 
 * @author eduglezexp
 */
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
