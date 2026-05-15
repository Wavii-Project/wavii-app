package com.wavii.dto.challenge;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO que consolida las estadísticas de progreso de un usuario.
 * Incluye racha actual, mejor racha histórica, XP acumulada, nivel y actividad mensual/semanal.
 */
public record StatsDto(
        int streak,
        int bestStreak,
        int xp,
        int level,
        List<LocalDate> completedDaysThisMonth,
        int completedThisWeek
) {}
