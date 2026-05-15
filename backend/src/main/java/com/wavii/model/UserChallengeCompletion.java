package com.wavii.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra la finalización de un desafío diario por parte de un usuario.
 * Sirve como historial de actividad para el cálculo de estadísticas y rachas.
 */
@Entity
@Table(
    name = "user_challenge_completions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "daily_challenge_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallengeCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_challenge_id", nullable = false)
    private DailyChallenge dailyChallenge;

    /** Fecha en la que se completo (para facilitar consultas del calendario) */
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime completedAt = LocalDateTime.now();
}
