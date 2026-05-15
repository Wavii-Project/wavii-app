package com.wavii.dto.challenge;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de estadisticas del usuario para la pantalla de progreso.
 * Incluye los dias completados en el mes solicitado y los datos de racha.
 */
public record StatsDto(
        int streak,
        int bestStreak,
        int xp,
        int level,
        List<LocalDate> completedDaysThisMonth,
        int completedThisWeek
) {}
