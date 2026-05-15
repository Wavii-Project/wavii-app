package com.wavii.model;

import com.wavii.model.enums.Level;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Representa un desafio diario: una tablatura asignada como desafio para una fecha concreta.
 * Cada dia se generan hasta 4 desafios por nivel de dificultad a partir de las
 * tablaturas subidas por la comunidad (PdfDocument).
 */
@Entity
@Table(
    name = "daily_challenges",
    uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_date", "difficulty", "slot"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fecha para la que es valido este desafio */
    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    /**
     * Dificultad del desafio: coincide con el nivel del usuario para el que esta pensado.
     * PRINCIPIANTE = 1, INTERMEDIO = 2, AVANZADO = 3
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level difficulty;

    /** Posicion del desafio dentro del bloque diario de su nivel (1..4) */
    private Integer slot;

    /** XP que otorga completar este desafio */
    @Column(nullable = false)
    private int xpReward;

    /** Tablatura de la comunidad asignada como desafio */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_document_id", nullable = false)
    private PdfDocument pdfDocument;
}
