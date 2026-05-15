package com.wavii.dto.onboarding;

import com.wavii.model.enums.Level;
import com.wavii.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompleteOnboardingRequest {

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotNull(message = "El nivel es obligatorio")
    private Level level;

    private String avatarUrl;
}
