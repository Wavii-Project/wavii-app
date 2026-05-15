package com.wavii.service;

import com.wavii.dto.challenge.CompleteChallengResponseDto;
import com.wavii.dto.challenge.DailyChallengeDto;
import com.wavii.dto.challenge.StatsDto;
import com.wavii.model.DailyChallenge;
import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import com.wavii.model.UserChallengeCompletion;
import com.wavii.model.enums.Level;
import com.wavii.repository.DailyChallengeRepository;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.UserChallengeCompletionRepository;
import com.wavii.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyChallengeService {

    private static final int CHALLENGES_PER_LEVEL = 4;

    // XP por dificultad
    private static final int XP_PRINCIPIANTE = 15;
    private static final int XP_INTERMEDIO   = 30;
    private static final int XP_AVANZADO     = 50;

    // XP necesario para pasar del nivel N al N+1: 100 * N^1.5
    // El nivel numerico interno va de 1 en adelante (no confundir con Level enum del onboarding)

    private final DailyChallengeRepository challengeRepo;
    private final PdfDocumentRepository pdfDocumentRepo;
    private final UserChallengeCompletionRepository completionRepo;
    private final UserRepository userRepo;

    // -------------------------------------------------------------------------
    // Generacion automatica de desafios del dia
    // -------------------------------------------------------------------------

    /**
     * Genera los desafios del dia si aun no existen.
     * Se llama desde el controller al pedir los desafios de hoy,
     * de forma que si no hay desafios generados (primer acceso del dia) se crean en ese momento.
     */
    @Transactional
    public void generateTodayChallengesIfNeeded() {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);

        for (Level level : Level.values()) {
            generateChallengesForLevelIfNeeded(today, level, sevenDaysAgo);
        }
    }

    // -------------------------------------------------------------------------
    // Obtener desafios del dia para un usuario
    // -------------------------------------------------------------------------

    /**
     * Devuelve los desafios de hoy visibles para el usuario segun su nivel:
     * - PRINCIPIANTE: solo desafios de nivel PRINCIPIANTE
     * - INTERMEDIO:   solo desafios de nivel INTERMEDIO
     * - AVANZADO:     solo desafios de nivel AVANZADO
     */
    @Transactional
    public List<DailyChallengeDto> getTodayChallengesForUser(User user) {
        generateTodayChallengesIfNeeded();

        LocalDate today = LocalDate.now();
        Level effectiveLevel = user.getLevel() != null ? user.getLevel() : Level.PRINCIPIANTE;
        List<DailyChallenge> all = challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(today, effectiveLevel);
        List<DailyChallengeDto> result = new ArrayList<>();

        for (DailyChallenge challenge : all) {
            boolean completed = completionRepo.existsByUserAndDailyChallenge(user, challenge);
            result.add(DailyChallengeDto.from(challenge, completed));
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Completar un desafio
    // -------------------------------------------------------------------------

    @Transactional
    public CompleteChallengResponseDto completeChallenge(Long challengeId, User user) {
        DailyChallenge challenge = challengeRepo.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Desafio no encontrado"));

        // Solo se puede completar el desafio del dia
        if (!challenge.getChallengeDate().equals(LocalDate.now())) {
            throw new IllegalStateException("Este desafio ya no esta disponible");
        }

        // No se puede completar dos veces
        if (completionRepo.existsByUserAndDailyChallenge(user, challenge)) {
            throw new IllegalStateException("Ya has completado este desafio hoy");
        }

        // El usuario debe tener acceso a esa dificultad
        if (!isVisibleForUser(challenge.getDifficulty(), user.getLevel())) {
            throw new IllegalStateException("No tienes acceso a este nivel de desafio");
        }

        // Registrar la completion
        UserChallengeCompletion completion = UserChallengeCompletion.builder()
                .user(user)
                .dailyChallenge(challenge)
                .completedDate(LocalDate.now())
                .build();
        completionRepo.save(completion);

        // Actualizar XP
        int xpGained = challenge.getXpReward();
        int oldXp    = user.getXp();
        int newXp    = oldXp + xpGained;
        int oldLevel = calculateLevel(oldXp);
        int newLevel = calculateLevel(newXp);
        boolean leveledUp = newLevel > oldLevel;

        user.setXp(newXp);

        // Actualizar racha
        updateStreak(user);

        userRepo.save(user);

        log.info("Usuario {} completo desafio {}. XP: {} -> {}, Racha: {}",
                user.getId(), challengeId, oldXp, newXp, user.getStreak());

        return new CompleteChallengResponseDto(xpGained, newXp, newLevel, leveledUp,
                user.getStreak(), user.getBestStreak());
    }

    // -------------------------------------------------------------------------
    // Estadisticas
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StatsDto getStats(User user) {
        LocalDate today     = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd   = today.with(TemporalAdjusters.lastDayOfMonth());

        List<LocalDate> completedThisMonth = completionRepo.findCompletedDatesByUserInRange(
                user.getId(), monthStart, monthEnd);

        // Dias completados en la semana actual (lunes a hoy)
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        List<LocalDate> completedThisWeek = completionRepo.findCompletedDatesByUserInRange(
                user.getId(), weekStart, today);

        return new StatsDto(
                user.getStreak(),
                user.getBestStreak(),
                user.getXp(),
                calculateLevel(user.getXp()),
                completedThisMonth,
                completedThisWeek.size()
        );
    }

    // -------------------------------------------------------------------------
    // Logica privada
    // -------------------------------------------------------------------------

    /**
     * Calcula el nivel numerico del usuario a partir de su XP total.
     * Formula: nivel N requiere 100 * N^1.5 XP acumulado desde el nivel anterior.
     * Se itera hasta que el XP acumulado supere el XP del usuario.
     */
    public static int calculateLevel(int xp) {
        int level = 1;
        int accumulated = 0;
        while (true) {
            int needed = (int) Math.round(100 * Math.pow(level, 1.5));
            if (accumulated + needed > xp) break;
            accumulated += needed;
            level++;
        }
        return level;
    }

    /**
     * Calcula el XP total necesario para alcanzar el nivel indicado (XP acumulado desde nivel 1).
     */
    public static int xpForLevel(int targetLevel) {
        int total = 0;
        for (int l = 1; l < targetLevel; l++) {
            total += (int) Math.round(100 * Math.pow(l, 1.5));
        }
        return total;
    }

    private static int xpForLevel(Level level) {
        return switch (level) {
            case PRINCIPIANTE -> XP_PRINCIPIANTE;
            case INTERMEDIO   -> XP_INTERMEDIO;
            case AVANZADO     -> XP_AVANZADO;
        };
    }

    private static int levelToInt(Level level) {
        return switch (level) {
            case PRINCIPIANTE -> 1;
            case INTERMEDIO   -> 2;
            case AVANZADO     -> 3;
        };
    }

    /**
     * Un usuario ve solo los desafios de su nivel actual.
     */
    private boolean isVisibleForUser(Level challengeLevel, Level userLevel) {
        if (userLevel == null) return challengeLevel == Level.PRINCIPIANTE;
        return challengeLevel == userLevel;
    }

    /**
     * Actualiza la racha del usuario:
     * - Si lastStreakDate es ayer: incrementa racha.
     * - Si lastStreakDate es hoy: ya conto, no hace nada.
     * - Cualquier otro caso (null, antes de ayer): reinicia a 1.
     * Actualiza bestStreak si se supera el record.
     */
    private void updateStreak(User user) {
        LocalDate today = LocalDate.now();
        LocalDate last  = user.getLastStreakDate();

        if (today.equals(last)) {
            // Ya se conto hoy, no cambia nada
            return;
        }

        if (last != null && last.equals(today.minusDays(1))) {
            // Dia consecutivo
            user.setStreak(user.getStreak() + 1);
        } else {
            // Fallo uno o mas dias, o primer desafio de siempre
            user.setStreak(1);
        }

        user.setLastStreakDate(today);

        if (user.getStreak() > user.getBestStreak()) {
            user.setBestStreak(user.getStreak());
        }
    }

    private void generateChallengesForLevelIfNeeded(LocalDate today, Level level, LocalDate sevenDaysAgo) {
        List<DailyChallenge> existing = challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(today, level);
        if (existing.size() >= CHALLENGES_PER_LEVEL) {
            return;
        }

        int difficultyInt = levelToInt(level);
        Set<Long> usedPdfIds = new HashSet<>();
        existing.forEach(challenge -> usedPdfIds.add(challenge.getPdfDocument().getId()));
        challengeRepo.findByChallengeDateGreaterThanEqual(sevenDaysAgo)
                .forEach(challenge -> usedPdfIds.add(challenge.getPdfDocument().getId()));

        List<PdfDocument> selectedTabs = pickTabsForDifficulty(difficultyInt, usedPdfIds, CHALLENGES_PER_LEVEL - existing.size());

        int nextSlot = existing.size() + 1;
        for (PdfDocument tab : selectedTabs) {
            if (nextSlot > CHALLENGES_PER_LEVEL) {
                break;
            }

            DailyChallenge challenge = DailyChallenge.builder()
                    .challengeDate(today)
                    .difficulty(level)
                    .slot(nextSlot)
                    .xpReward(xpForLevel(level))
                    .pdfDocument(tab)
                    .build();

            challengeRepo.save(challenge);
            log.info("Desafio generado para {} en slot {} con tablatura {}", level, nextSlot, tab.getId());
            nextSlot++;
        }

        if (existing.size() + selectedTabs.size() < CHALLENGES_PER_LEVEL) {
            log.warn("Solo se pudieron generar {} desafios para nivel {} en {}",
                    existing.size() + selectedTabs.size(), level, today);
        }
    }

    private void collectTabs(List<PdfDocument> selectedTabs, Set<Long> usedPdfIds, List<PdfDocument> candidates, int targetSize) {
        for (PdfDocument candidate : candidates) {
            if (usedPdfIds.add(candidate.getId())) {
                selectedTabs.add(candidate);
            }
            if (selectedTabs.size() >= targetSize) {
                return;
            }
        }
    }

    private List<PdfDocument> pickTabsForDifficulty(int difficultyInt, Set<Long> recentlyUsedPdfIds, int needed) {
        if (needed <= 0) {
            return List.of();
        }

        List<PdfDocument> allCandidates = new ArrayList<>(pdfDocumentRepo.findAllByDifficultyWithOwner(difficultyInt));
        Collections.shuffle(allCandidates);

        List<PdfDocument> freshCandidates = new ArrayList<>();
        List<PdfDocument> fallbackCandidates = new ArrayList<>();

        for (PdfDocument candidate : allCandidates) {
            if (recentlyUsedPdfIds.contains(candidate.getId())) {
                fallbackCandidates.add(candidate);
            } else {
                freshCandidates.add(candidate);
            }
        }

        List<PdfDocument> selectedTabs = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        collectTabs(selectedTabs, selectedIds, freshCandidates, needed);

        if (selectedTabs.size() < needed) {
            collectTabs(selectedTabs, selectedIds, fallbackCandidates, needed);
        }

        return selectedTabs;
    }
}
