package com.wavii.service;

import com.wavii.dto.challenge.CompleteChallengResponseDto;
import com.wavii.dto.challenge.DailyChallengeDto;
import com.wavii.dto.challenge.StatsDto;
import com.wavii.model.DailyChallenge;
import com.wavii.model.PdfDocument;
import com.wavii.model.User;
import com.wavii.model.enums.Level;
import com.wavii.repository.DailyChallengeRepository;
import com.wavii.repository.PdfDocumentRepository;
import com.wavii.repository.UserChallengeCompletionRepository;
import com.wavii.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengeServiceTest {

    @Mock private DailyChallengeRepository challengeRepo;
    @Mock private PdfDocumentRepository pdfDocumentRepo;
    @Mock private UserChallengeCompletionRepository completionRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks private DailyChallengeService service;

    private User user;
    private PdfDocument pdf;
    private DailyChallenge challengePrincipiante;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setLevel(Level.PRINCIPIANTE);
        user.setXp(0);
        user.setStreak(0);
        user.setBestStreak(0);
        user.setLastStreakDate(null);

        pdf = PdfDocument.builder()
                .id(1L)
                .originalName("test.pdf")
                .fileName("test.pdf")
                .filePath("/path/test.pdf")
                .songTitle("Test Song")
                .difficulty(1)
                .likeCount(0)
                .owner(user)
                .build();

        challengePrincipiante = DailyChallenge.builder()
                .id(1L)
                .challengeDate(LocalDate.now())
                .difficulty(Level.PRINCIPIANTE)
                .slot(1)
                .xpReward(15)
                .pdfDocument(pdf)
                .build();
    }

    // ─── generateTodayChallengesIfNeeded ──────────────────────────

    @Test
    void generateTodayChallengesIfNeededAlreadyExistsDoesNotGenerateTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any())).thenReturn(List.of(
                buildChallenge(1L, Level.PRINCIPIANTE, 15, 1),
                buildChallenge(2L, Level.PRINCIPIANTE, 15, 2),
                buildChallenge(3L, Level.PRINCIPIANTE, 15, 3),
                buildChallenge(4L, Level.PRINCIPIANTE, 15, 4)
        ));

        service.generateTodayChallengesIfNeeded();

        verify(pdfDocumentRepo, never()).findAllByDifficultyWithOwner(anyInt());
        verify(challengeRepo, never()).save(any());
    }

    @Test
    void generateTodayChallengesIfNeededNotExistsGeneratesForAllLevelsTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any())).thenReturn(List.of());
        when(challengeRepo.findByChallengeDateGreaterThanEqual(any())).thenReturn(List.of());
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(anyInt()))
                .thenReturn(List.of(
                        buildPdf(11L), buildPdf(12L), buildPdf(13L), buildPdf(14L)
                ));
        when(challengeRepo.save(any())).thenReturn(challengePrincipiante);

        service.generateTodayChallengesIfNeeded();

        verify(pdfDocumentRepo, times(Level.values().length)).findAllByDifficultyWithOwner(anyInt());
        verify(challengeRepo, times(Level.values().length * 4)).save(any());
    }

    @Test
    void generateTodayChallengesIfNeededNoTabsAvailableSkipsAllLevelsTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any())).thenReturn(List.of());
        when(challengeRepo.findByChallengeDateGreaterThanEqual(any())).thenReturn(List.of());
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(anyInt())).thenReturn(List.of());

        service.generateTodayChallengesIfNeeded();

        verify(challengeRepo, never()).save(any(DailyChallenge.class));
    }

    @Test
    void generateTodayChallengesIfNeededSomeLevelsNoTabsSavesOnlyAvailableTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any())).thenReturn(List.of());
        when(challengeRepo.findByChallengeDateGreaterThanEqual(any())).thenReturn(List.of());
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(1))).thenReturn(List.of(
                buildPdf(21L), buildPdf(22L), buildPdf(23L), buildPdf(24L)
        ));
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(2))).thenReturn(List.of());
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(3))).thenReturn(List.of());
        when(challengeRepo.save(any())).thenReturn(challengePrincipiante);

        service.generateTodayChallengesIfNeeded();

        verify(challengeRepo, times(4)).save(any());
    }

    @Test
    void generateTodayChallengesIfNeededUsesFallbackPoolWhenRecentPoolIsShortTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any())).thenReturn(List.of());
        when(challengeRepo.findByChallengeDateGreaterThanEqual(any())).thenReturn(List.of(
                buildChallengeWithPdf(99L, Level.PRINCIPIANTE, 15, 1, buildPdf(31L))
        ));
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(1))).thenReturn(List.of(
                buildPdf(31L), buildPdf(32L), buildPdf(33L), buildPdf(34L)
        ));
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(2))).thenReturn(List.of(
                buildPdf(41L), buildPdf(42L), buildPdf(43L), buildPdf(44L)
        ));
        when(pdfDocumentRepo.findAllByDifficultyWithOwner(eq(3))).thenReturn(List.of(
                buildPdf(51L), buildPdf(52L), buildPdf(53L), buildPdf(54L)
        ));

        service.generateTodayChallengesIfNeeded();

        verify(pdfDocumentRepo).findAllByDifficultyWithOwner(1);
    }

    // ─── getTodayChallengesForUser ────────────────────────────────

    @Test
    void getTodayChallengesForUserPrincipianteSeesOnlyPrincipianteTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), eq(Level.PRINCIPIANTE)))
                .thenReturn(List.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(eq(user), any())).thenReturn(false);

        List<DailyChallengeDto> result = service.getTodayChallengesForUser(user);

        assertEquals(1, result.size());
        assertEquals(Level.PRINCIPIANTE, result.get(0).difficulty());
        assertFalse(result.get(0).completedByMe());
    }

    @Test
    void getTodayChallengesForUserIntermedioSeesPrincipianteAndIntermedioTest() {
        user.setLevel(Level.INTERMEDIO);
        DailyChallenge intermedio1 = buildChallenge(2L, Level.INTERMEDIO, 30, 1);
        DailyChallenge intermedio2 = buildChallenge(3L, Level.INTERMEDIO, 30, 2);
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any()))
                .thenAnswer(invocation -> {
                    Level level = invocation.getArgument(1);
                    if (level == Level.INTERMEDIO) {
                        return List.of(intermedio1, intermedio2);
                    }
                    return List.of(
                            buildChallenge(100L, level, 15, 1), buildChallenge(101L, level, 15, 2),
                            buildChallenge(102L, level, 15, 3), buildChallenge(103L, level, 15, 4));
                });
        when(completionRepo.existsByUserAndDailyChallenge(eq(user), any())).thenReturn(false);

        List<DailyChallengeDto> result = service.getTodayChallengesForUser(user);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(challenge -> challenge.difficulty() == Level.INTERMEDIO));
    }

    @Test
    void getTodayChallengesForUserAvanzadoSeesAllTest() {
        user.setLevel(Level.AVANZADO);
        DailyChallenge avanzado1 = buildChallenge(3L, Level.AVANZADO, 50, 1);
        DailyChallenge avanzado2 = buildChallenge(4L, Level.AVANZADO, 50, 2);
        DailyChallenge avanzado3 = buildChallenge(5L, Level.AVANZADO, 50, 3);
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), any()))
                .thenAnswer(invocation -> {
                    Level level = invocation.getArgument(1);
                    if (level == Level.AVANZADO) {
                        return List.of(avanzado1, avanzado2, avanzado3);
                    }
                    return List.of(
                            buildChallenge(100L, level, 15, 1), buildChallenge(101L, level, 15, 2),
                            buildChallenge(102L, level, 15, 3), buildChallenge(103L, level, 15, 4));
                });
        when(completionRepo.existsByUserAndDailyChallenge(eq(user), any())).thenReturn(false);

        List<DailyChallengeDto> result = service.getTodayChallengesForUser(user);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(challenge -> challenge.difficulty() == Level.AVANZADO));
    }

    @Test
    void getTodayChallengesForUserNullLevelSeesOnlyPrincipianteTest() {
        user.setLevel(null);
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), eq(Level.PRINCIPIANTE)))
                .thenReturn(List.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(eq(user), eq(challengePrincipiante))).thenReturn(false);

        List<DailyChallengeDto> result = service.getTodayChallengesForUser(user);

        assertEquals(1, result.size());
        assertEquals(Level.PRINCIPIANTE, result.get(0).difficulty());
    }

    @Test
    void getTodayChallengesForUserAlreadyCompletedMarksCompletedByMeTest() {
        when(challengeRepo.findByChallengeDateAndDifficultyOrderBySlotAsc(any(), eq(Level.PRINCIPIANTE)))
                .thenReturn(List.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(true);

        List<DailyChallengeDto> result = service.getTodayChallengesForUser(user);

        assertEquals(1, result.size());
        assertTrue(result.get(0).completedByMe());
    }

    // ─── completeChallenge ────────────────────────────────────────

    @Test
    void completeChallengeSuccessUpdatesXpAndStreakTest() {
        user.setLastStreakDate(LocalDate.now().minusDays(1));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        CompleteChallengResponseDto result = service.completeChallenge(1L, user);

        assertNotNull(result);
        assertEquals(15, result.xpGained());
        assertEquals(15, result.totalXp());
        assertEquals(1, result.streak());
        assertFalse(result.leveledUp());
        verify(completionRepo).save(any());
        verify(userRepo).save(user);
    }

    @Test
    void completeChallengeNotFoundThrowsIllegalArgumentTest() {
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.completeChallenge(99L, user));
    }

    @Test
    void completeChallengeOldChallengeThrowsIllegalStateTest() {
        challengePrincipiante.setChallengeDate(LocalDate.now().minusDays(1));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));

        assertThrows(IllegalStateException.class, () -> service.completeChallenge(1L, user));
    }

    @Test
    void completeChallengeAlreadyCompletedThrowsIllegalStateTest() {
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.completeChallenge(1L, user));
    }

    @Test
    void completeChallengeLevelTooHighThrowsIllegalStateTest() {
        DailyChallenge avanzado = buildChallenge(2L, Level.AVANZADO, 50, 1);
        when(challengeRepo.findById(2L)).thenReturn(Optional.of(avanzado));
        when(completionRepo.existsByUserAndDailyChallenge(user, avanzado)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.completeChallenge(2L, user));
    }

    @Test
    void completeChallengeSameDayStreakDoesNotIncrementTest() {
        user.setStreak(5);
        user.setLastStreakDate(LocalDate.now());
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        CompleteChallengResponseDto result = service.completeChallenge(1L, user);

        assertEquals(5, result.streak());
    }

    @Test
    void completeChallengeStreakBrokenResetsToOneTest() {
        user.setStreak(3);
        user.setLastStreakDate(LocalDate.now().minusDays(3));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        CompleteChallengResponseDto result = service.completeChallenge(1L, user);

        assertEquals(1, result.streak());
    }

    @Test
    void completeChallengeFirstEverNullLastStreakSetsStreakToOneTest() {
        user.setLastStreakDate(null);
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        CompleteChallengResponseDto result = service.completeChallenge(1L, user);

        assertEquals(1, result.streak());
    }

    @Test
    void completeChallengeBestStreakUpdatedWhenNewRecordTest() {
        user.setStreak(5);
        user.setBestStreak(5);
        user.setLastStreakDate(LocalDate.now().minusDays(1));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        service.completeChallenge(1L, user);

        assertEquals(6, user.getBestStreak());
    }

    @Test
    void completeChallengeLevelUpSetsLeveledUpTrueTest() {
        user.setXp(99);
        user.setLastStreakDate(null);
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(challengePrincipiante));
        when(completionRepo.existsByUserAndDailyChallenge(user, challengePrincipiante)).thenReturn(false);
        when(userRepo.save(user)).thenReturn(user);

        CompleteChallengResponseDto result = service.completeChallenge(1L, user);

        assertTrue(result.leveledUp());
    }

    // ─── getStats ─────────────────────────────────────────────────

    @Test
    void getStatsReturnsCorrectStatsTest() {
        user.setStreak(3);
        user.setBestStreak(7);
        user.setXp(150);
        when(completionRepo.findCompletedDatesByUserInRange(eq(user.getId()), any(), any()))
                .thenReturn(List.of(LocalDate.now()));

        StatsDto result = service.getStats(user);

        assertNotNull(result);
        assertEquals(3, result.streak());
        assertEquals(7, result.bestStreak());
        assertEquals(150, result.xp());
        assertNotNull(result.completedDaysThisMonth());
    }

    @Test
    void getStatsWeekAndMonthCallsTest() {
        when(completionRepo.findCompletedDatesByUserInRange(eq(user.getId()), any(), any()))
                .thenReturn(List.of());

        service.getStats(user);

        verify(completionRepo, times(2)).findCompletedDatesByUserInRange(eq(user.getId()), any(), any());
    }

    // ─── calculateLevel (static) ──────────────────────────────────

    @Test
    void calculateLevelZeroXpReturnsLevel1Test() {
        assertEquals(1, DailyChallengeService.calculateLevel(0));
    }

    @Test
    void calculateLevelJustBelow100ReturnsLevel1Test() {
        assertEquals(1, DailyChallengeService.calculateLevel(99));
    }

    @Test
    void calculateLevelExactly100ReturnsLevel2Test() {
        assertEquals(2, DailyChallengeService.calculateLevel(100));
    }

    @Test
    void calculateLevelHighXpReturnsHigherLevelTest() {
        int level = DailyChallengeService.calculateLevel(10000);
        assertTrue(level > 5);
    }

    // ─── xpForLevel (static) ─────────────────────────────────────

    @Test
    void xpForLevelLevel1Returns0Test() {
        assertEquals(0, DailyChallengeService.xpForLevel(1));
    }

    @Test
    void xpForLevelLevel2Returns100Test() {
        assertEquals(100, DailyChallengeService.xpForLevel(2));
    }

    @Test
    void xpForLevelIncreasingLevelsXpGrowsTest() {
        assertTrue(DailyChallengeService.xpForLevel(3) > DailyChallengeService.xpForLevel(2));
        assertTrue(DailyChallengeService.xpForLevel(4) > DailyChallengeService.xpForLevel(3));
    }

    // ─── helpers ─────────────────────────────────────────────────

    private DailyChallenge buildChallenge(Long id, Level level, int xp, int slot) {
        return DailyChallenge.builder()
                .id(id)
                .challengeDate(LocalDate.now())
                .difficulty(level)
                .slot(slot)
                .xpReward(xp)
                .pdfDocument(pdf)
                .build();
    }

    private PdfDocument buildPdf(Long id) {
        return PdfDocument.builder()
                .id(id)
                .originalName("song-" + id + ".pdf")
                .fileName("song-" + id + ".pdf")
                .filePath("/path/song-" + id + ".pdf")
                .songTitle("Song " + id)
                .difficulty(1)
                .likeCount(0)
                .owner(user)
                .build();
    }

    private DailyChallenge buildChallengeWithPdf(Long id, Level level, int xp, int slot, PdfDocument challengePdf) {
        return DailyChallenge.builder()
                .id(id)
                .challengeDate(LocalDate.now())
                .difficulty(level)
                .slot(slot)
                .xpReward(xp)
                .pdfDocument(challengePdf)
                .build();
    }
}
