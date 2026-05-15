package com.wavii.dto.user;

import com.wavii.model.User;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO que representa el perfil público de un usuario.
 * No expone datos sensibles como el email o el teléfono de contacto.
 */
public record PublicUserProfileDto(
        UUID id,
        String name,
        String level,           // "PRINCIPIANTE" | "INTERMEDIO" | "AVANZADO"
        String role,            // "USUARIO" | "PROFESOR_PARTICULAR" | "PROFESOR_CERTIFICADO"
        int xp,
        int streak,
        int bestStreak,
        int tabsPublished,
        boolean acceptsMessages,
        String memberSince      // "2026-05" (año-mes)
) {
    public static PublicUserProfileDto from(User user, int tabsPublished) {
        return new PublicUserProfileDto(
                user.getId(),
                user.getName(),
                user.getLevel() != null ? user.getLevel().name() : null,
                user.getRole() != null ? user.getRole().name() : null,
                user.getXp(),
                user.getStreak(),
                user.getBestStreak(),
                tabsPublished,
                user.isAcceptsMessages(),
                user.getCreatedAt() != null
                        ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                        : null
        );
    }
}
