package com.wavii.dto.challenge;

/**
 * Respuesta tras completar un desafio.
 * Devuelve el estado actualizado del usuario para que el frontend pueda
 * reflejar los cambios sin necesidad de una segunda llamada.
 */
public record CompleteChallengResponseDto(
        int xpGained,
        int totalXp,
        int newLevel,
        boolean leveledUp,
        int streak,
        int bestStreak
) {}
